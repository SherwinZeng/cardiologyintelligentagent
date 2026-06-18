<script setup lang="ts">
import { Delete, Top } from '@element-plus/icons-vue';
import { computed, ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';

import type { ChatSessionItem } from '../types';

const ACTION_WIDTH = 76;
const SWIPE_THRESHOLD = 28;
const DRAG_LOCK_PX = 6;

const props = defineProps<{
  session: ChatSessionItem;
  active: boolean;
  open: boolean;
}>();

const emit = defineEmits<{
  select: [];
  delete: [];
  pin: [];
  openChange: [open: boolean];
}>();

const { t } = useI18n();

const offsetX = ref(0);
const dragging = ref(false);
const pointerStartX = ref(0);
const pointerStartY = ref(0);
const offsetStart = ref(0);
const moved = ref(false);
const horizontalLocked = ref(false);
const verticalLocked = ref(false);

const contentStyle = computed(() => ({
  transform: `translateX(${offsetX.value}px)`,
}));

const pinLabel = computed(() =>
  props.session.pinned ? t('chat.unpinSession') : t('chat.pinSession'),
);

watch(
  () => props.open,
  (open) => {
    offsetX.value = open ? -ACTION_WIDTH : 0;
  },
  { immediate: true },
);

function clampOffset(value: number) {
  return Math.max(-ACTION_WIDTH, Math.min(0, value));
}

function snapOpen(open: boolean) {
  offsetX.value = open ? -ACTION_WIDTH : 0;
  emit('openChange', open);
}

function onPointerDown(event: MouseEvent | TouchEvent) {
  const clientX = 'touches' in event ? event.touches[0].clientX : event.clientX;
  const clientY = 'touches' in event ? event.touches[0].clientY : event.clientY;
  dragging.value = true;
  moved.value = false;
  horizontalLocked.value = false;
  verticalLocked.value = false;
  pointerStartX.value = clientX;
  pointerStartY.value = clientY;
  offsetStart.value = offsetX.value;

  if (!('touches' in event)) {
    document.addEventListener('mousemove', onDocumentMouseMove);
    document.addEventListener('mouseup', onDocumentMouseUp);
  }
}

function onDocumentMouseMove(event: MouseEvent) {
  onPointerMove(event);
}

function onDocumentMouseUp() {
  document.removeEventListener('mousemove', onDocumentMouseMove);
  document.removeEventListener('mouseup', onDocumentMouseUp);
  onPointerEnd();
}

function onPointerMove(event: MouseEvent | TouchEvent) {
  if (!dragging.value) {
    return;
  }

  const clientX = 'touches' in event ? event.touches[0].clientX : event.clientX;
  const clientY = 'touches' in event ? event.touches[0].clientY : event.clientY;
  const deltaX = clientX - pointerStartX.value;
  const deltaY = clientY - pointerStartY.value;

  if (!horizontalLocked.value && !verticalLocked.value) {
    if (Math.abs(deltaX) < DRAG_LOCK_PX && Math.abs(deltaY) < DRAG_LOCK_PX) {
      return;
    }
    if (Math.abs(deltaY) > Math.abs(deltaX)) {
      verticalLocked.value = true;
      dragging.value = false;
      return;
    }
    horizontalLocked.value = true;
  }

  if (verticalLocked.value || !horizontalLocked.value) {
    return;
  }

  if (Math.abs(deltaX) > DRAG_LOCK_PX) {
    moved.value = true;
  }
  offsetX.value = clampOffset(offsetStart.value + deltaX);
}

function onPointerEnd() {
  if (!dragging.value) {
    return;
  }
  dragging.value = false;
  snapOpen(offsetX.value <= -SWIPE_THRESHOLD);
}

function onClickItem() {
  if (moved.value) {
    moved.value = false;
    return;
  }
  if (props.open) {
    snapOpen(false);
    return;
  }
  emit('select');
}

function onPinClick() {
  emit('pin');
  snapOpen(false);
}

function onDeleteClick() {
  emit('delete');
  snapOpen(false);
}
</script>

<template>
  <li class="chat-session-swipe">
    <div class="chat-session-swipe__actions" aria-hidden="true">
      <button
        type="button"
        class="chat-session-swipe__action chat-session-swipe__action--pin"
        :aria-label="pinLabel"
        :title="pinLabel"
        @click.stop="onPinClick"
      >
        <el-icon><Top /></el-icon>
      </button>
      <button
        type="button"
        class="chat-session-swipe__action chat-session-swipe__action--delete"
        :aria-label="t('chat.deleteSession')"
        :title="t('chat.deleteSession')"
        @click.stop="onDeleteClick"
      >
        <el-icon><Delete /></el-icon>
      </button>
    </div>

    <div
      class="chat-session-swipe__content"
      :class="{ 'is-dragging': dragging }"
      :style="contentStyle"
      @mousedown="onPointerDown"
      @touchstart.passive="onPointerDown"
      @touchmove.passive="onPointerMove"
      @touchend="onPointerEnd"
      @click="onClickItem"
    >
      <article
        class="chat-session-aside__item"
        :class="{ 'is-active': active, 'is-pinned': session.pinned }"
      >
        <div class="chat-session-aside__item-head">
          <span class="chat-session-aside__item-title">
            <el-icon v-if="session.pinned" class="chat-session-aside__pin-icon"><Top /></el-icon>
            {{ session.title }}
          </span>
          <time class="chat-session-aside__item-time">{{ session.updatedAt }}</time>
        </div>
        <p class="chat-session-aside__item-preview">
          {{ session.preview || '暂无消息' }}
        </p>
        <div class="chat-session-aside__item-foot">
          <span class="chat-session-aside__item-meta">
            {{ t('records.messages', { n: session.messageCount }) }}
          </span>
          <span
            v-if="session.status"
            class="chat-session-aside__item-status"
            :class="`is-${session.status}`"
          >
            {{ session.status === 'active' ? '进行中' : session.status }}
          </span>
        </div>
      </article>
    </div>
  </li>
</template>

<style scoped lang="scss">
@use './styles/chat-session-swipe-item.scss';
</style>
