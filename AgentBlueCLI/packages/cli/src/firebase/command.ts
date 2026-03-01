import {
  collection,
  addDoc,
  doc,
  onSnapshot,
  serverTimestamp,
  type Unsubscribe,
} from 'firebase/firestore';
import { getDb, getCurrentUser } from './client.js';

export interface StepLog {
  step: number;
  actionType: string;
  targetText?: string;
  reasoning?: string;
  success: boolean;
  timestamp: number;
}

export interface AgentState {
  status: 'IDLE' | 'RUNNING' | 'COMPLETED' | 'FAILED' | 'CANCELLED';
  currentCommand: string;
  currentStep: number;
  maxSteps: number;
  currentReasoning: string;
  liveSteps: StepLog[];
  updatedAt?: { seconds: number };
}

export async function sendCommand(sessionId: string, command: string): Promise<string> {
  const db = getDb();
  const user = getCurrentUser();

  const ref = await addDoc(collection(db, 'sessions', sessionId, 'commands'), {
    command,
    status: 'pending',
    deviceId: user.uid,
    result: null,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  });

  return ref.id;
}

export function listenAgentState(
  sessionId: string,
  onUpdate: (state: AgentState) => void,
): Unsubscribe {
  const db = getDb();
  return onSnapshot(
    doc(db, 'sessions', sessionId, 'agentState', 'current'),
    (snap) => {
      if (snap.exists()) {
        onUpdate(snap.data() as AgentState);
      }
    },
  );
}

export function listenCommandResult(
  sessionId: string,
  commandId: string,
  onResult: (status: 'completed' | 'failed', result?: string) => void,
): Unsubscribe {
  const db = getDb();
  return onSnapshot(doc(db, 'sessions', sessionId, 'commands', commandId), (snap) => {
    const data = snap.data();
    if (!data) return;
    const status = data['status'] as string;
    if (status === 'completed' || status === 'failed') {
      onResult(status, data['result'] as string | undefined);
    }
  });
}
