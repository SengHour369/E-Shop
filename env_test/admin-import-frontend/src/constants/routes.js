// src/constants/routes.js
// Re-exports PATHS from app/routes/routePaths for convenience.
// Use PATHS directly from routePaths in most cases; this barrel is for
// non-route contexts (e.g. nav config) that don't want to import from app/.
export { PATHS, default } from '../app/routes/routePaths';
