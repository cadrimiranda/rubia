import ChatPage from "./pages/ChatPage";
import { ProtectedRoute } from "./components/ProtectedRoute";
import ErrorBoundary from "./components/ErrorBoundary";
import { AuthProvider } from "./components/AuthContext";

function App() {
  return (
    <ErrorBoundary>
      <AuthProvider>
        <ProtectedRoute requiredRole="AGENT">
          <ChatPage />
        </ProtectedRoute>
      </AuthProvider>
    </ErrorBoundary>
  );
}

export default App;
