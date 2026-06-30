export function decodeToken(token) {
  if (!token) return null;
  try {
    const payload = token.split('.')[1];
    const json = atob(payload.replace(/-/g, '+').replace(/_/g, '/'));
    return JSON.parse(decodeURIComponent(escape(json)));
  } catch {
    return null;
  }
}

export function getCurrentRole() {
  const token = localStorage.getItem('token');
  const payload = decodeToken(token);
  return payload?.role || null;
}
