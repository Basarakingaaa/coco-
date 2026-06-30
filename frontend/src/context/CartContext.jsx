import { createContext, useCallback, useContext, useEffect, useState } from 'react';
import { cartApi } from '../api';

const CartContext = createContext(null);

export function CartProvider({ children }) {
  const [count, setCount] = useState(0);

  const refresh = useCallback(async () => {
    const token = localStorage.getItem('token');
    if (!token) {
      setCount(0);
      return;
    }
    try {
      const items = await cartApi.list();
      const total = (items.data || []).reduce((sum, item) => sum + item.quantity, 0);
      setCount(total);
    } catch {
      setCount(0);
    }
  }, []);

  useEffect(() => {
    refresh();
  }, [refresh]);

  return (
    <CartContext.Provider value={{ count, refresh }}>
      {children}
    </CartContext.Provider>
  );
}

export function useCart() {
  return useContext(CartContext);
}
