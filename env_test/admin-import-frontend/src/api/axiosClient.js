import axios from "axios";
import { ENV } from "../config/env";
import { setupInterceptors } from "./interceptors";

const axiosClient = axios.create({
    // baseURL : ENV.APP_API_URL_LOCAL,
    baseURL : ENV.APP_API_URL,
    timeout : ENV.TIMEOUT,
    headers :{
        'Content-Type': 'application/json',
        Accept : 'application/json',
    },
});

// Register intercept
setupInterceptors(axiosClient);

export default axiosClient;