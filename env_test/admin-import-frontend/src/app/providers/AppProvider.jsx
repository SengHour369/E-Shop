// src/app/providers/AppProvider.jsx
// Wraps the entire application with all global context providers.
// Order: AuthProvider → UIProvider → (future providers go here)
import { BrowserRouter } from 'react-router-dom';
import { AuthProvider } from '@/contexts/AuthContext';
import { UIProvider } from '@/contexts/UIContext';

export default function AppProvider({ children }) {
  return (
    <BrowserRouter>
      <AuthProvider>
        <UIProvider>
          {children}
        </UIProvider>
      </AuthProvider>
    </BrowserRouter>
  );
}
