import { createContext, useContext, useState, useEffect } from 'react';
import api from '../services/api';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [token, setToken] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    try {
      const storedToken = localStorage.getItem('queuesense_token');
      const storedUser = localStorage.getItem('queuesense_user');
      if (storedToken && storedUser) {
        setToken(storedToken);
        setUser(JSON.parse(storedUser));
      }
    } catch (e) {
      console.error('Failed to parse stored session:', e);
      localStorage.removeItem('queuesense_token');
      localStorage.removeItem('queuesense_user');
    } finally {
      setLoading(false);
    }
  }, []);

  const login = async (email, password) => {
    const response = await api.post('/auth/login', { email, password });
    const data = response.data;
    
    const userData = {
      id: data.id,
      email: data.email,
      name: data.name,
      role: data.role,
      studentId: data.studentId,
      assignedFacilityId: data.assignedFacilityId,
      assignedFacilityName: data.assignedFacilityName,
    };

    localStorage.setItem('queuesense_token', data.token);
    localStorage.setItem('queuesense_user', JSON.stringify(userData));

    setToken(data.token);
    setUser(userData);
    return userData;
  };

  const logout = () => {
    localStorage.removeItem('queuesense_token');
    localStorage.removeItem('queuesense_user');
    setToken(null);
    setUser(null);
  };

  const isStudent = user?.role === 'ROLE_STUDENT';
  const isOperator = user?.role === 'ROLE_OPERATOR';
  const isAuthenticated = !!token && !!user;

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        loading,
        isAuthenticated,
        isStudent,
        isOperator,
        login,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
