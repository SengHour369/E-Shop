export default function PageContainer({ children, className = '' }) {
  return (
    <div className={`flex flex-col gap-6 w-full ${className}`}>
      {children}
    </div>
  );
}
