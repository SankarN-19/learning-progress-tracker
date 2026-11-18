import { Route, Routes } from 'react-router-dom';
import { PublicOnly, RequireAuth, RequirePasswordChange } from './auth/guards';
import Layout from './components/Layout';
import LandingPage from './pages/LandingPage';
import LoginPage from './pages/LoginPage';
import ChangePasswordPage from './pages/ChangePasswordPage';
import DashboardPage from './pages/DashboardPage';
import ActivitiesPage from './pages/ActivitiesPage';
import LearnersPage from './pages/LearnersPage';
import LearnerDetailPage from './pages/LearnerDetailPage';
import MyTrainingPage from './pages/MyTrainingPage';
import NotFoundPage from './pages/NotFoundPage';

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<LandingPage />} />

      <Route element={<PublicOnly />}>
        <Route path="/login" element={<LoginPage />} />
      </Route>

      <Route element={<RequirePasswordChange />}>
        <Route path="/change-password" element={<ChangePasswordPage />} />
      </Route>

      <Route element={<RequireAuth roles={['COORDINATOR']} />}>
        <Route element={<Layout />}>
          <Route path="/dashboard" element={<DashboardPage />} />
          <Route path="/activities" element={<ActivitiesPage />} />
          <Route path="/learners" element={<LearnersPage />} />
          <Route path="/learners/:id" element={<LearnerDetailPage />} />
        </Route>
      </Route>

      <Route element={<RequireAuth roles={['CANDIDATE']} />}>
        <Route element={<Layout />}>
          <Route path="/my-training" element={<MyTrainingPage />} />
        </Route>
      </Route>

      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}
