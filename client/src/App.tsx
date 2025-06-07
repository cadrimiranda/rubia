import ChatPage from './pages/ChatPage'
import ProtectedRoute from './components/ProtectedRoute'
import ErrorBoundary from './components/ErrorBoundary'
import { ToastProvider } from './components/notifications/ToastProvider'
import 'antd/dist/reset.css'

function App() {
  return (
    <ErrorBoundary>
      <ToastProvider>
        <ProtectedRoute requiredRole="AGENT">
          <ChatPage />
        </ProtectedRoute>
      </ToastProvider>
    </ErrorBoundary>
  )
}

export default App
