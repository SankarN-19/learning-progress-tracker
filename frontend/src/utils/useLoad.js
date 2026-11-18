import { useCallback, useEffect, useRef, useState } from 'react';
import { errorMessage } from '../api/client';

/**
 * Loads data and exposes { data, loading, error, reload }.
 * reload() keeps the old data on screen while refreshing, so lists do not flicker after a save.
 */
export default function useLoad(loader, deps = []) {
  const [state, setState] = useState({ data: null, loading: true, error: '' });
  const alive = useRef(true);

  useEffect(() => {
    alive.current = true;
    return () => {
      alive.current = false;
    };
  }, []);

  // eslint-disable-next-line react-hooks/exhaustive-deps
  const reload = useCallback(async () => {
    setState((s) => ({ ...s, loading: true, error: '' }));
    try {
      const data = await loader();
      if (alive.current) setState({ data, loading: false, error: '' });
    } catch (err) {
      if (alive.current) setState({ data: null, loading: false, error: errorMessage(err, 'Could not load data.') });
    }
  }, deps);

  useEffect(() => {
    reload();
  }, [reload]);

  return { ...state, reload };
}
