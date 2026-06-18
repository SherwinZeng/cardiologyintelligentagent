import { createRouter, createWebHistory } from 'vue-router';

import AppLayout from '@/layouts/AppLayout.vue';
import { useUserLoginStore } from '@/stores/login';
import { resolvePostLoginRedirect } from '@/utils/resolveLoginRedirect';
import AppointmentView from '@/views/appointment/AppointmentView.vue';
import ChatView from '@/views/chat/ChatView.vue';
import HelpView from '@/views/help/HelpView.vue';
import LoginView from '@/views/login/LoginView.vue';
import PrivacyView from '@/views/privacy/PrivacyView.vue';
import RecordsView from '@/views/records/RecordsView.vue';
import ReportsView from '@/views/reports/ReportsView.vue';
import WelcomeView from '@/views/welcome/WelcomeView.vue';

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: LoginView,
    },
    {
      path: '/',
      component: AppLayout,
      children: [
        {
          path: '',
          name: 'home',
          component: WelcomeView,
        },
        {
          path: 'chat',
          name: 'chat',
          component: ChatView,
        },
        {
          path: 'records',
          name: 'records',
          component: RecordsView,
        },
        {
          path: 'reports',
          name: 'reports',
          component: ReportsView,
        },
        {
          path: 'appointment',
          name: 'appointment',
          component: AppointmentView,
        },
        {
          path: 'help',
          name: 'help',
          component: HelpView,
        },
        {
          path: 'privacy',
          name: 'privacy',
          component: PrivacyView,
        },
      ],
    },
  ],
});

router.beforeEach((to) => {
  if (to.name !== 'login') {
    return true;
  }

  const loginStore = useUserLoginStore();
  if (!loginStore.isLoggedIn) {
    return true;
  }

  return resolvePostLoginRedirect(to.query.redirect);
});

export default router;
