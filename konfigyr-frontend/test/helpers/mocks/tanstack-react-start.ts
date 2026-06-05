export const createServerFnMock = () => {
  const chain = {
    inputValidator() {
      return chain;
    },
    handler(handler: (...args: Array<any>) => Promise<any> | any) {
      return async (...args: Array<any>) => await handler(...args);
    },
  };

  return chain;
};

export const mockTanstackReactStart = () => {
  return {
    createServerFn: createServerFnMock,
  };
};
