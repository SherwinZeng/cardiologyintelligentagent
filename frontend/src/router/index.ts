import { createRouter, createWebHistory } from 'vue-router'

import AppLayout from '@/layouts/AppLayout.vue'
import { useUserLoginStore } from '@/stores/login'
import { resolvePostLoginRedirect } from '@/utils/resolveLoginRedirect'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/login/LoginView.vue'),
    },
    {
      path: '/',
      component: AppLayout,
      children: [
        {
          path: '',
          name: 'home',
          component: () => import('@/views/welcome/WelcomeView.vue'),
        },
        {
          path: 'chat',
          name: 'chat',
          component: () => import('@/views/chat/ChatView.vue'),
        },
        {
          path: 'records',
          name: 'records',
          component: () => import('@/views/records/RecordsView.vue'),
        },
        {
          path: 'reports',
          name: 'reports',
          component: () => import('@/views/reports/ReportsView.vue'),
        },
        {
          path: 'appointment',
          name: 'appointment',
          component: () => import('@/views/appointment/AppointmentView.vue'),
        },
        {
          path: 'help',
          name: 'help',
          component: () => import('@/views/help/HelpView.vue'),
        },
        {
          path: 'privacy',
          name: 'privacy',
          component: () => import('@/views/privacy/PrivacyView.vue'),
        },
      ],
    },
  ],
})

router.beforeEach((to) => {
  if (to.name !== 'login') {
    return true
  }

  const loginStore = useUserLoginStore()
  if (!loginStore.isLoggedIn) {
    return true
  }

  return resolvePostLoginRedirect(to.query.redirect)
})

export default router
