import { createRouter, createWebHistory } from 'vue-router'

import AppLayout from '@/layouts/AppLayout.vue'

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
      ],
    },
  ],
})

export default router
