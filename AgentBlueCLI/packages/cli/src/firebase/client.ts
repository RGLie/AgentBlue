import { initializeApp, FirebaseApp, getApps } from 'firebase/app';
import { getAuth, signInAnonymously, Auth, User } from 'firebase/auth';
import { getFirestore, Firestore } from 'firebase/firestore';
import { getConfig } from '../config.js';

let app: FirebaseApp;
let db: Firestore;
let auth: Auth;
let currentUser: User | null = null;

export async function initFirebase(): Promise<{ db: Firestore; user: User }> {
  const config = getConfig();

  if (getApps().length === 0) {
    app = initializeApp(config.firebase);
  } else {
    app = getApps()[0]!;
  }

  db = getFirestore(app);
  auth = getAuth(app);

  const credential = await signInAnonymously(auth);
  currentUser = credential.user;

  return { db, user: currentUser };
}

export function getDb(): Firestore {
  if (!db) {
    throw new Error('Firebase가 초기화되지 않았습니다. initFirebase()를 먼저 호출하세요.');
  }
  return db;
}

export function getCurrentUser(): User {
  if (!currentUser) {
    throw new Error('인증되지 않은 상태입니다. initFirebase()를 먼저 호출하세요.');
  }
  return currentUser;
}
