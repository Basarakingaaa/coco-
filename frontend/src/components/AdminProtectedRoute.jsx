import { Navigate } from 'react-router-dom';
import { getCurrentRole } from '../utils/jwt.js';

export default function AdminProtectedRoute({ children }) {
  const role = getCurrentRole();
  if (role !== 'ADMIN') {
    return <Navigate to="/login" replace />;
  }
  return children;
}
