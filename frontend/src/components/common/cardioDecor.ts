export type CardioDecorMotion = 'float' | 'heartbeat' | 'drift' | 'spin' | 'glow';
export type CardioDecorType = 'heart' | 'stethoscope' | 'pulse' | 'bp-cuff' | 'pill' | 'cross';

export interface CardioDecorItem {
  id: string;
  type: CardioDecorType;
  top: string;
  left: string;
  size: number;
  motion: CardioDecorMotion;
  delay: number;
}

/** 聊天空状态：环绕铭铭四周的心内小装饰 */
export const CHAT_EMPTY_DECOR_ITEMS: CardioDecorItem[] = [
  { id: 'c01', type: 'heart', top: '7%', left: '7%', size: 26, motion: 'heartbeat', delay: 0 },
  { id: 'c02', type: 'pulse', top: '10%', left: '20%', size: 28, motion: 'drift', delay: 0.5 },
  { id: 'c03', type: 'stethoscope', top: '8%', left: '86%', size: 26, motion: 'float', delay: 1.1 },
  { id: 'c04', type: 'pill', top: '12%', left: '72%', size: 22, motion: 'spin', delay: 0.3 },
  { id: 'c05', type: 'heart', top: '18%', left: '92%', size: 20, motion: 'heartbeat', delay: 1.8 },
  { id: 'c06', type: 'cross', top: '28%', left: '4%', size: 20, motion: 'glow', delay: 0.8 },
  { id: 'c07', type: 'bp-cuff', top: '32%', left: '14%', size: 26, motion: 'float', delay: 1.5 },
  { id: 'c08', type: 'pulse', top: '30%', left: '90%', size: 30, motion: 'drift', delay: 2.2 },
  { id: 'c09', type: 'stethoscope', top: '62%', left: '6%', size: 24, motion: 'float', delay: 0.6 },
  { id: 'c10', type: 'heart', top: '70%', left: '18%', size: 22, motion: 'heartbeat', delay: 2.4 },
  { id: 'c11', type: 'pill', top: '68%', left: '88%', size: 22, motion: 'spin', delay: 1.2 },
  { id: 'c12', type: 'pulse', top: '78%', left: '78%', size: 28, motion: 'drift', delay: 1.9 },
  { id: 'c13', type: 'cross', top: '82%', left: '10%', size: 18, motion: 'glow', delay: 2.6 },
  { id: 'c14', type: 'bp-cuff', top: '84%', left: '92%', size: 24, motion: 'float', delay: 0.4 },
];
