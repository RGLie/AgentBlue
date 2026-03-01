import {
  doc,
  setDoc,
  getDoc,
  updateDoc,
  onSnapshot,
  collection,
  query,
  where,
  getDocs,
  serverTimestamp,
  type Unsubscribe,
} from 'firebase/firestore';
import { getDb, getCurrentUser } from './client.js';

// 혼동 가능 문자(0, O, 1, I, L) 제외한 문자 세트 — Android 앱과 동일
const CODE_CHARS = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';

function generateCode(length = 8): string {
  return Array.from(
    { length },
    () => CODE_CHARS[Math.floor(Math.random() * CODE_CHARS.length)],
  ).join('');
}

async function isCodeAvailable(code: string): Promise<boolean> {
  const db = getDb();
  const q = query(
    collection(db, 'sessions'),
    where('code', '==', code),
    where('status', 'in', ['waiting', 'paired']),
  );
  const snap = await getDocs(q);
  return snap.empty;
}

export async function createSession(): Promise<{ sessionId: string; code: string }> {
  const db = getDb();
  const user = getCurrentUser();

  let code: string;
  do {
    code = generateCode();
  } while (!(await isCodeAvailable(code)));

  const sessionRef = doc(collection(db, 'sessions'));
  const sessionId = sessionRef.id;

  await setDoc(sessionRef, {
    code,
    desktopUid: user.uid,
    androidUid: null,
    status: 'waiting',
    createdAt: serverTimestamp(),
  });

  return { sessionId, code };
}

export async function getSessionByCode(code: string): Promise<{ sessionId: string } | null> {
  const db = getDb();
  const q = query(
    collection(db, 'sessions'),
    where('code', '==', code.toUpperCase()),
    where('status', 'in', ['waiting', 'paired']),
  );
  const snap = await getDocs(q);
  if (snap.empty) return null;
  return { sessionId: snap.docs[0]!.id };
}

export function listenSessionStatus(
  sessionId: string,
  onPaired: (androidUid: string) => void,
  onDisconnected: () => void,
): Unsubscribe {
  const db = getDb();
  return onSnapshot(doc(db, 'sessions', sessionId), (snap) => {
    const data = snap.data();
    if (!data) return;
    if (data['status'] === 'paired' && data['androidUid']) {
      onPaired(data['androidUid'] as string);
    }
    if (data['status'] === 'disconnected') {
      onDisconnected();
    }
  });
}

export async function disconnectSession(sessionId: string): Promise<void> {
  const db = getDb();
  await updateDoc(doc(db, 'sessions', sessionId), { status: 'disconnected' });
}

export async function checkSessionPaired(sessionId: string): Promise<boolean> {
  const db = getDb();
  const snap = await getDoc(doc(db, 'sessions', sessionId));
  if (!snap.exists()) return false;
  return snap.data()?.['status'] === 'paired';
}
