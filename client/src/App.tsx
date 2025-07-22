import { BrowserRouter as Router, Routes, Route, Navigate } from "react-router-dom";
import ChatPage from "./pages/ChatPage";
import LoginPage from "./pages/LoginPage";
import ZApiActivation from "./components/ZApiActivation";
import { ProtectedRoute } from "./components/ProtectedRoute";
import ErrorBoundary from "./components/ErrorBoundary";
import { AuthProvider } from "./components/AuthContext";

function App() {
  return (
    <ErrorBoundary>
      <AuthProvider>
        <Router>
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route 
              path="/" 
              element={
                <ProtectedRoute requiredRole="AGENT">
                  <ChatPage />
                </ProtectedRoute>
              } 
            />
            <Route 
              path="/zapi-activation" 
              element={
                <ProtectedRoute requiredRole="ADMIN">
                  <ZApiActivation />
                </ProtectedRoute>
              } 
            />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </Router>
      </AuthProvider>
    </ErrorBoundary>
  );
}

export default App;
