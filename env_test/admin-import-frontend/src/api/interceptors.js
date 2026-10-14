// Request interceptor
export const setupInterceptors = (axiosClients) =>{
    axiosClients.interceptors.request.use(
        (config) => {
            const token = localStorage.getItem("accessToken");
            
            if (token){
                config.headers.Authorization = `Bearer ${token}`;
            }
            return config;
        },
        (error) => Promise.reject(error)
    );

    // Response interceptor
    axiosClients.interceptors.response.use(
        (response) => response.data,
        (error) => {
            switch(error.response?.status){
                case 401:
                case 403:
                    localStorage.removeItem("accessToken");
                    if (window.location.pathname !== '/' && window.location.pathname !== '/login') {
                        window.location.href = "/login";
                    }
                    break;
                case 404:
                    // window.location.href = "/404";
                    break;
                case 500:
                    // window.location.href = "/500";
                    break;
            }
            return Promise.reject(error);
        }
    );
};