import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { Layout } from "./components/Layout";
import { AuthProvider } from "./context/AuthContext";
import { AdminPage } from "./pages/AdminPage";
import { ArticlePage } from "./pages/ArticlePage";
import { ArticlesPage } from "./pages/ArticlesPage";
import { DigestPage } from "./pages/DigestPage";

export default function App() {
	return (
		<AuthProvider>
			<BrowserRouter>
				<Routes>
					<Route element={<Layout />}>
						<Route path="/" element={<DigestPage />} />
						<Route path="/articles" element={<ArticlesPage />} />
						<Route path="/articles/:id" element={<ArticlePage />} />
						<Route path="/admin" element={<AdminPage />} />
						<Route path="*" element={<Navigate to="/" replace />} />
					</Route>
				</Routes>
			</BrowserRouter>
		</AuthProvider>
	);
}
