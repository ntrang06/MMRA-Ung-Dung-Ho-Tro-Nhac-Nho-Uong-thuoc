import { initializeApp } from "firebase/app";
import { getAuth, GoogleAuthProvider } from "firebase/auth";

const firebaseConfig = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY || "AIzaSyCdfTDG5s8HVDPu1rhLrSL7_3j_wJlviK4",
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN || "mmra-dev.firebaseapp.com",
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID || "mmra-dev",
  storageBucket: import.meta.env.VITE_FIREBASE_STORAGE_BUCKET || "mmra-dev.firebasestorage.app",
  messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID || "572045438815",
  appId: import.meta.env.VITE_FIREBASE_APP_ID || "1:572045438815:web:scaffold"
};

const app = initializeApp(firebaseConfig);
export const auth = getAuth(app);
export const googleProvider = new GoogleAuthProvider();
