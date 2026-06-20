<script setup lang="ts">
import { ArrowRight, Calendar, Search } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { computed, onMounted, reactive, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRouter } from 'vue-router';

import { getApiErrorMessage } from '@/http';
import { useRequireAuth } from '@/hooks/useRequireAuth.ts';
import { fetchConsultationRecordsService } from '@/services/record/fetchConsultationRecords.ts';
import { useUserLoginStore } from '@/stores/login.ts';
import type { IBaseResponse } from '@/typings/baseResponse.ts';
import type {
  IConsultationRecordPageResponse,
  IConsultationRecordResponse,
} from '@/typings/record/consultationRecord.ts';

import ConsultationRecordIcon from './components/ConsultationRecordIcon.vue';

import mingmingQAvatarUrl from '@/assets/character/mingming-welcome-q.png';

const { t } = useI18n();
const router = useRouter();
const { requireAuth } = useRequireAuth();
const { userLoginStore } = useUserLoginStore();

const PAGE_SIZE = 5;
const dateRange = ref<[string, string] | []>([]);
const records = ref<IConsultationRecordResponse[]>([]);
const loading = ref(false);
const total = ref(0);
const detailVisible = ref(false);
const detailRecord = ref<IConsultationRecordResponse | null>(null);
const filters = reactive({
  page: 1,
  urgency: 'all',
  keyword: '',
});

const hasRecords = computed(() => records.value.length > 0);
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / PAGE_SIZE)));

function formatDateTime(value?: string | null): string {
  if (!value) {
    return '';
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  const h = String(date.getHours()).padStart(2, '0');
  const min = String(date.getMinutes()).padStart(2, '0');
  return `${y}-${m}-${d} ${h}:${min}`;
}

function formatTableDateTime(value?: string | null): string {
  if (!value) {
    return '';
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  const h = String(date.getHours()).padStart(2, '0');
  const min = String(date.getMinutes()).padStart(2, '0');
  return `${m}-${d} ${h}:${min}`;
}

function urgencyClass(urgency?: string) {
  if (urgency === 'red') return 'is-high';
  if (urgency === 'yellow') return 'is-moderate';
  return 'is-low';
}

function urgencyText(urgency?: string) {
  if (urgency === 'red') return t('urgency.high');
  if (urgency === 'yellow') return t('urgency.moderate');
  return t('urgency.low');
}

async function loadRecords() {
  if (!requireAuth({ notify: false })) {
    return;
  }
  loading.value = true;
  try {
    const [startDate, endDate] = dateRange.value;
    const response = await fetchConsultationRecordsService<
      IBaseResponse<IConsultationRecordPageResponse>
    >({
      uid: userLoginStore.id,
      page: filters.page,
      pageSize: PAGE_SIZE,
      urgency: filters.urgency === 'all' ? undefined : filters.urgency,
      keyword: filters.keyword.trim() || undefined,
      startDate,
      endDate,
    });

    if (response.code !== 200 || !response.data) {
      ElMessage.error(response.message || t('records.loadFailed'));
      return;
    }

    records.value = response.data.records ?? [];
    total.value = response.data.total ?? 0;
    filters.page = response.data.page ?? filters.page;
  } catch (error) {
    ElMessage.error(getApiErrorMessage(error));
  } finally {
    loading.value = false;
  }
}

function resetToFirstPage() {
  filters.page = 1;
  void loadRecords();
}

function handlePageChange(page: number) {
  filters.page = page;
  void loadRecords();
}

function handleContinue(sessionId: string) {
  void router.push({ name: 'chat', query: { session: sessionId } });
}

function handleViewDetail(record: IConsultationRecordResponse) {
  detailRecord.value = record;
  detailVisible.value = true;
}

function handleDetailContinue() {
  if (!detailRecord.value?.sessionId) {
    return;
  }
  detailVisible.value = false;
  handleContinue(detailRecord.value.sessionId);
}

onMounted(() => {
  void loadRecords();
});
</script>

<template>
  <section class="records-page">
    <div v-loading="loading" class="records-page__panel">
      <header class="records-page__header">
        <h1 class="records-page__title">{{ t('records.title') }}</h1>
      </header>

      <div class="records-page__filters">
        <div class="records-page__filter-item records-page__filter-date">
          <el-icon class="records-page__filter-icon"><Calendar /></el-icon>
          <el-date-picker
            v-model="dateRange"
            class="records-page__date"
            type="daterange"
            value-format="YYYY-MM-DD"
            :start-placeholder="t('records.startDate')"
            :end-placeholder="t('records.endDate')"
            :prefix-icon="''"
            @change="resetToFirstPage"
          />
        </div>

        <el-select
          v-model="filters.urgency"
          class="records-page__filter-item records-page__urgency"
          :placeholder="t('urgency.all')"
          @change="resetToFirstPage"
        >
          <el-option :label="t('urgency.all')" value="all" />
          <el-option :label="t('urgency.low')" value="green" />
          <el-option :label="t('urgency.moderate')" value="yellow" />
          <el-option :label="t('urgency.high')" value="red" />
        </el-select>

        <div class="records-page__filter-item records-page__filter-search">
          <el-icon class="records-page__filter-icon"><Search /></el-icon>
          <el-input
            v-model="filters.keyword"
            class="records-page__search"
            clearable
            :placeholder="t('records.search')"
            @change="resetToFirstPage"
            @clear="resetToFirstPage"
          />
        </div>
      </div>

      <section class="records-page__table-section">
        <div class="records-page__list">
          <template v-if="hasRecords">
            <div class="records-page__list-head records-page__row">
              <span class="records-page__col-topic">{{ t('records.colTopic') }}</span>
              <span class="records-page__col-urgency">{{ t('records.colUrgency') }}</span>
              <span class="records-page__col-time">{{ t('records.colEndedAt') }}</span>
              <span class="records-page__col-count">{{ t('records.colMessages') }}</span>
              <span class="records-page__col-summary">{{ t('records.colSummary') }}</span>
              <span class="records-page__col-actions">{{ t('records.colActions') }}</span>
            </div>

            <article v-for="record in records" :key="record.id" class="records-page__item records-page__row">
              <div class="records-page__topic-cell records-page__col-topic">
                <ConsultationRecordIcon compact :title="record.title" />
                <h2 class="records-page__record-title">{{ record.title }}</h2>
              </div>

              <span
                class="records-page__tag records-page__col-urgency"
                :class="urgencyClass(record.urgency)"
              >
                {{ urgencyText(record.urgency) }}
              </span>

              <span class="records-page__col-time">{{ formatTableDateTime(record.endedAt) }}</span>

              <span class="records-page__col-count">
                {{ t('records.messageCount', { n: record.messageCount }) }}
              </span>

              <p class="records-page__summary records-page__col-summary">{{ record.summary }}</p>

              <div class="records-page__actions records-page__col-actions">
                <button
                  type="button"
                  class="records-page__continue"
                  @click="handleContinue(record.sessionId)"
                >
                  {{ t('records.continue') }}
                </button>
                <button
                  type="button"
                  class="records-page__detail"
                  @click="handleViewDetail(record)"
                >
                  {{ t('records.detail') }}
                  <el-icon><ArrowRight /></el-icon>
                </button>
              </div>
            </article>
          </template>

          <el-empty v-else class="records-page__empty" :description="t('records.recordEmpty')" />
        </div>

        <footer v-if="total > 0" class="records-page__pagination">
          <el-pagination
            background
            layout="prev, pager, next"
            :current-page="filters.page"
            :page-size="PAGE_SIZE"
            :total="total"
            @current-change="handlePageChange"
          />
          <span class="records-page__pagination-meta">
            {{ t('records.totalPages', { pages: totalPages, total }) }}
          </span>
        </footer>
      </section>

      <aside class="records-page__hint" aria-label="records generation rules">
        <p class="records-page__hint-title">{{ t('records.generationHintTitle') }}</p>
        <ul class="records-page__hint-list">
          <li>{{ t('records.generationHintLogin') }}</li>
          <li>{{ t('records.generationHintMessages') }}</li>
          <li>{{ t('records.generationHintIdle') }}</li>
          <li>{{ t('records.generationHintOngoing') }}</li>
        </ul>
      </aside>

      <aside class="records-page__hint" aria-label="system archiving tips">
        <p class="records-page__hint-title">{{ t('records.systemTipsTitle') }}</p>
        <ul class="records-page__hint-list">
          <li>{{ t('records.archivingTipAccount') }}</li>
          <li>{{ t('records.archivingTipWhen') }}</li>
          <li>{{ t('records.archivingTipEndedAt') }}</li>
        </ul>
      </aside>
    </div>

    <el-drawer
      v-model="detailVisible"
      class="records-page__drawer"
      :title="detailRecord?.title || t('records.detail')"
      size="480px"
      destroy-on-close
    >
      <template v-if="detailRecord">
        <div class="records-page__drawer-body">
          <div class="records-page__drawer-head">
            <ConsultationRecordIcon :title="detailRecord.title" />
            <div class="records-page__drawer-meta">
              <span class="records-page__tag" :class="urgencyClass(detailRecord.urgency)">
                {{ urgencyText(detailRecord.urgency) }}
              </span>
              <p class="records-page__drawer-time">{{ formatDateTime(detailRecord.endedAt) }}</p>
              <p class="records-page__drawer-count">
                {{ t('records.messages', { n: detailRecord.messageCount }) }}
              </p>
            </div>
          </div>

          <div class="records-page__drawer-section">
            <h3>{{ t('records.summaryLabel') }}</h3>
            <p>{{ detailRecord.summary }}</p>
          </div>

          <div class="records-page__drawer-actions">
            <el-button type="primary" size="large" @click="handleDetailContinue">
              {{ t('records.continue') }}
            </el-button>
          </div>

          <div class="records-page__drawer-brand" aria-hidden="true">
            <div class="records-page__drawer-brand-inner">
              <span class="records-page__drawer-brand-aura" />
              <img
                class="records-page__drawer-brand-logo"
                :src="mingmingQAvatarUrl"
                :alt="t('ai.assistant')"
              />
            </div>
          </div>
        </div>
      </template>
    </el-drawer>
  </section>
</template>

<style scoped lang="scss">
@use './styles/records-view.scss';
</style>
