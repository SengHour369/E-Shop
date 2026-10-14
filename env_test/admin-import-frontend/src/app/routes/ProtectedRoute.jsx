import { Navigate, Outlet } from "react-router-dom";

const ProtectRoute =()=>{
    const Token = localStorage.getItem('token');
    console.log("Checking token in ProtectedRoute:", Token);

    if(!Token){
        return <Navigate to={"/login"} replace />;
    }
    return <Outlet />;
};

export default ProtectRoute;
