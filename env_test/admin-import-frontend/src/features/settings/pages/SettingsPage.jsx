import SettingsForm from '../components/SettingsForm';
import { PageHeader } from '@/components/ui';
import PageContainer from '@/components/layouts/PageContainer';


export default function SettingsPage() {
  return (
    <PageContainer>
      <PageHeader 
        title="Settings" 
        crumbs={[{ label: 'Dashboard', path: '/' }, { label: 'Settings' }]}
      />
      <div className="max-w-3xl">
        <SettingsForm />
      </div>
    </PageContainer>
  );
}
