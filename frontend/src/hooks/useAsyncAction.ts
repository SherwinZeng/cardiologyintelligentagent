export const useAsyncAction = () => {
  const run = async <T>(
    task: () => Promise<T>,
    onSuccess: (data: T) => void,
    onError: (error: unknown) => void,
  ) => {
    try {
      const taskResponse = await task();
      onSuccess && onSuccess(taskResponse);
    } catch (error) {
      onError && onError(error);
    }
  };

  return {
    run,
  };
};
