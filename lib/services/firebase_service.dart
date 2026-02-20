import 'dart:async';

import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:firebase_auth/firebase_auth.dart';
import 'package:firebase_core/firebase_core.dart';
import 'package:uuid/uuid.dart';

// [중요] 이 import 문을 추가해야 합니다.
// (파일 경로에 따라 ../firebase_options.dart 일 수도 있습니다)
import '../firebase_options.dart';

class FirebaseService {
  FirebaseService._();

  static final FirebaseService instance = FirebaseService._();

  // [권장 수정] 초기화 전에 인스턴스에 접근하면 에러가 날 수 있으므로
  // late final 또는 getter로 변경하는 것이 안전합니다.
  late final FirebaseFirestore _firestore = FirebaseFirestore.instance;
  late final FirebaseAuth _auth = FirebaseAuth.instance;
  final _uuid = const Uuid();

  StreamSubscription<DocumentSnapshot<Map<String, dynamic>>>? _resultSub;
  String? _deviceId;

  // uuid 생성을 한 번만 하도록 수정 (null check operator 활용)
  String get deviceId => _deviceId ??= _uuid.v4();

  Future<void> initialize() async {
    if (Firebase.apps.isEmpty) {
      // [핵심 수정 부분] options 파라미터 추가
      await Firebase.initializeApp(
        options: DefaultFirebaseOptions.currentPlatform,
      );
    }

    // 초기화가 끝난 후 인스턴스에 접근해야 안전합니다.
    if (_auth.currentUser == null) {
      await _auth.signInAnonymously();
    }
  }

  Future<String> sendCommand({required String command}) async {
    final docRef = _firestore.collection('commands').doc();

    await docRef.set({
      'command': command,
      'status': 'pending', // 초기 상태
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
      }) {
    _resultSub?.cancel();

    // snapshots() 스트림 구독
    _resultSub = _firestore
        .collection('commands')
        .doc(commandId)
        .snapshots(includeMetadataChanges: true)
        .listen((snapshot) {

      if (!snapshot.exists) return;

      final data = snapshot.data();
      if (data == null) return;

      final status = data['status'] as String? ?? '';
      final result = data['result'] as String? ?? '';

      // 상태가 변경되었을 때 콜백 실행 및 리스너 해제
      if (status == 'completed') {
        onCompleted(result);
        _resultSub?.cancel();
      } else if (status == 'failed') {
        onFailed(result);
        _resultSub?.cancel();
      }
    });
  }
}