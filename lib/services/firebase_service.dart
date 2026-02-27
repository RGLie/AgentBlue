import 'dart:async';
import 'dart:math';

import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:firebase_auth/firebase_auth.dart';
import 'package:firebase_core/firebase_core.dart';
import 'package:uuid/uuid.dart';

import '../firebase_options.dart';

class AgentState {
  final String status;
  final String? currentCommand;
  final int currentStep;
  final int maxSteps;
  final String? currentReasoning;
  final List<Map<String, dynamic>> liveSteps;

  const AgentState({
    this.status = 'IDLE',
    this.currentCommand,
    this.currentStep = 0,
    this.maxSteps = 0,
    this.currentReasoning,
    this.liveSteps = const [],
  });

  bool get isRunning => status == 'RUNNING';
  bool get isIdle => status == 'IDLE';
  bool get isCompleted => status == 'COMPLETED';
  bool get isFailed => status == 'FAILED';
  bool get isCancelled => status == 'CANCELLED';

  factory AgentState.fromMap(Map<String, dynamic> data) {
    final rawSteps = data['liveSteps'];
    final steps = <Map<String, dynamic>>[];
    if (rawSteps is List) {
      for (final s in rawSteps) {
        if (s is Map<String, dynamic>) steps.add(s);
      }
    }

    return AgentState(
      status: data['status'] as String? ?? 'IDLE',
      currentCommand: data['currentCommand'] as String?,
      currentStep: (data['currentStep'] as num?)?.toInt() ?? 0,
      maxSteps: (data['maxSteps'] as num?)?.toInt() ?? 0,
      currentReasoning: data['currentReasoning'] as String?,
      liveSteps: steps,
    );
  }
}

class CommandRecord {
  final String id;
  final String command;
  final String status;
  final String result;
  final DateTime? createdAt;

  const CommandRecord({
    required this.id,
    required this.command,
    required this.status,
    required this.result,
    this.createdAt,
  });

  factory CommandRecord.fromDoc(DocumentSnapshot<Map<String, dynamic>> doc) {
    final data = doc.data() ?? {};
    final ts = data['createdAt'] as Timestamp?;
    return CommandRecord(
      id: doc.id,
      command: data['command'] as String? ?? '',
      status: data['status'] as String? ?? 'pending',
      result: data['result'] as String? ?? '',
      createdAt: ts?.toDate(),
    );
  }
}

class FirebaseService {
  FirebaseService._();

  static final FirebaseService instance = FirebaseService._();

  late final FirebaseFirestore _firestore = FirebaseFirestore.instance;
  late final FirebaseAuth _auth = FirebaseAuth.instance;
  final _uuid = const Uuid();
  final _random = Random.secure();

  StreamSubscription<DocumentSnapshot<Map<String, dynamic>>>? _resultSub;
  StreamSubscription<DocumentSnapshot<Map<String, dynamic>>>? _sessionSub;
  StreamSubscription<DocumentSnapshot<Map<String, dynamic>>>? _agentStateSub;
  StreamSubscription<QuerySnapshot<Map<String, dynamic>>>? _historySub;

  String? _deviceId;
  String? _sessionId;
  String? _sessionCode;

  String get deviceId => _deviceId ??= _uuid.v4();
  String? get sessionId => _sessionId;
  String? get sessionCode => _sessionCode;
  bool get hasSession => _sessionId != null;
  String? get currentUid => _auth.currentUser?.uid;

  Future<void> initialize() async {
    if (Firebase.apps.isEmpty) {
      await Firebase.initializeApp(
        options: DefaultFirebaseOptions.currentPlatform,
      );
    }

    if (_auth.currentUser == null) {
      await _auth.signInAnonymously();
    }

    await _restoreSession();
  }

  Future<void> _restoreSession() async {
    final uid = _auth.currentUser?.uid;
    if (uid == null) return;

    try {
      final snapshots = await _firestore
          .collection('sessions')
          .where('desktopUid', isEqualTo: uid)
          .get();

      for (final doc in snapshots.docs) {
        final status = doc.data()['status'] as String? ?? '';
        if (status == 'waiting' || status == 'paired') {
          _sessionId = doc.id;
          _sessionCode = doc.data()['code'] as String?;
          return;
        }
      }
    } catch (_) {
      // 세션 복원 실패 시 무시 (새 세션을 생성하면 됨)
    }
  }

  // --- Session Management ---

  String _generateCode() {
    const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
    return List.generate(8, (_) => chars[_random.nextInt(chars.length)]).join();
  }

  Future<String> createSession() async {
    final code = _generateCode();
    final uid = _auth.currentUser?.uid;

    final existing = await _firestore
        .collection('sessions')
        .where('code', isEqualTo: code)
        .where('status', isEqualTo: 'waiting')
        .get();

    if (existing.docs.isNotEmpty) {
      return createSession();
    }

    final docRef = _firestore.collection('sessions').doc();
    await docRef.set({
      'code': code,
      'desktopUid': uid,
      'androidUid': null,
      'status': 'waiting',
      'createdAt': FieldValue.serverTimestamp(),
    });

    _sessionId = docRef.id;
    _sessionCode = code;
    return code;
  }

  void listenSessionStatus({
    required void Function() onPaired,
    required void Function() onDisconnected,
  }) {
    if (_sessionId == null) return;

    _sessionSub?.cancel();
    _sessionSub = _firestore
        .collection('sessions')
        .doc(_sessionId)
        .snapshots()
        .listen((snapshot) {
      if (!snapshot.exists) return;
      final data = snapshot.data();
      if (data == null) return;

      final status = data['status'] as String? ?? '';
      if (status == 'paired') {
        onPaired();
      } else if (status == 'disconnected') {
        onDisconnected();
      }
    });
  }

  Future<String?> getSessionStatus() async {
    if (_sessionId == null) return null;

    final doc = await _firestore.collection('sessions').doc(_sessionId).get();
    if (!doc.exists) return null;
    return doc.data()?['status'] as String?;
  }

  Future<void> disconnectSession() async {
    if (_sessionId == null) return;

    await _firestore.collection('sessions').doc(_sessionId).update({
      'status': 'disconnected',
    });

    _sessionSub?.cancel();
    _sessionSub = null;
    _agentStateSub?.cancel();
    _agentStateSub = null;
    _historySub?.cancel();
    _historySub = null;
    _sessionId = null;
    _sessionCode = null;
  }

  // --- Commands ---

  Future<String> sendCommand({required String command}) async {
    final String path;
    if (_sessionId != null) {
      path = 'sessions/$_sessionId/commands';
    } else {
      path = 'commands';
    }

    final docRef = _firestore.collection(path).doc();

    await docRef.set({
      'command': command,
      'status': 'pending',
      'result': '',
      'deviceId': deviceId,
      'createdAt': FieldValue.serverTimestamp(),
      'updatedAt': FieldValue.serverTimestamp(),
    });

    return docRef.id;
  }

  void listenCommandResult(
    String commandId, {
    required void Function(String result) onCompleted,
    required void Function(String result) onFailed,
    void Function()? onProcessing,
  }) {
    _resultSub?.cancel();

    final String path;
    if (_sessionId != null) {
      path = 'sessions/$_sessionId/commands';
    } else {
      path = 'commands';
    }

    _resultSub = _firestore
        .collection(path)
        .doc(commandId)
        .snapshots(includeMetadataChanges: true)
        .listen((snapshot) {
      if (!snapshot.exists) return;

      final data = snapshot.data();
      if (data == null) return;

      final status = data['status'] as String? ?? '';
      final result = data['result'] as String? ?? '';

      if (status == 'processing') {
        onProcessing?.call();
      } else if (status == 'completed') {
        onCompleted(result);
        _resultSub?.cancel();
      } else if (status == 'failed') {
        onFailed(result);
        _resultSub?.cancel();
      }
    });
  }

  // --- Agent State Listening ---

  Stream<AgentState> agentStateStream() {
    if (_sessionId == null) {
      return const Stream.empty();
    }

    return _firestore
        .collection('sessions')
        .doc(_sessionId)
        .collection('agentState')
        .doc('current')
        .snapshots()
        .map((snapshot) {
      if (!snapshot.exists || snapshot.data() == null) {
        return const AgentState();
      }
      return AgentState.fromMap(snapshot.data()!);
    });
  }

  // --- Command History ---

  Stream<List<CommandRecord>> commandHistoryStream() {
    if (_sessionId == null) {
      return const Stream.empty();
    }

    return _firestore
        .collection('sessions')
        .doc(_sessionId)
        .collection('commands')
        .orderBy('createdAt', descending: true)
        .limit(50)
        .snapshots()
        .map((snapshot) {
      return snapshot.docs.map((doc) => CommandRecord.fromDoc(doc)).toList();
    });
  }

  void dispose() {
    _resultSub?.cancel();
    _sessionSub?.cancel();
    _agentStateSub?.cancel();
    _historySub?.cancel();
  }
}
