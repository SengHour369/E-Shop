/**
 * Class name utility — joins truthy class strings and resolves object maps.
 *
 * Supports both call styles:
 *   cn('base', condition && 'conditional')          ← string / falsy
 *   cn('base', { 'class-a': true, 'class-b': false })  ← object map
 */
export function cn(...args) {
  const classes = [];

  for (const arg of args) {
    if (!arg) continue;

    if (typeof arg === 'string') {
      classes.push(arg);
    } else if (typeof arg === 'object') {
      for (const [key, value] of Object.entries(arg)) {
        if (value) classes.push(key);
      }
    }
  }

  return classes.join(' ');
}
