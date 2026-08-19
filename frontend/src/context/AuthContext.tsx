import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from "react";
import { getToken, setToken } from "../lib/auth";

type AuthContextValue = {
	token: string | null;
	signedIn: boolean;
	signIn: (token: string) => void;
	signOut: () => void;
};

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
	const [token, setTokenState] = useState<string | null>(() => getToken());
	const signIn = useCallback((next: string) => {
		setToken(next);
		setTokenState(next);
	}, []);
	const signOut = useCallback(() => {
		setToken(null);
		setTokenState(null);
	}, []);
	const value = useMemo(
		() => ({ token, signedIn: Boolean(token), signIn, signOut }),
		[token, signIn, signOut]
	);
	return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
	const ctx = useContext(AuthContext);
	if (!ctx) {
		throw new Error("useAuth must be used inside AuthProvider");
	}
	return ctx;
}
