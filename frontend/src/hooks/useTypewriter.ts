import { onUnmounted, ref } from 'vue';

interface UseTypewriterOptions {
  speed?: number;
}

export function useTypewriter(options: UseTypewriterOptions = {}) {
  const displayedText = ref('');
  const isTyping = ref(false);
  const speed = options.speed ?? 22;

  let timer: number | undefined;
  let fullText = '';
  let index = 0;
  let onTick: (() => void) | undefined;

  function clearTimer() {
    if (timer !== undefined) {
      window.clearInterval(timer);
      timer = undefined;
    }
  }

  function finish() {
    clearTimer();
    displayedText.value = fullText;
    isTyping.value = false;
  }

  function start(text: string, tick?: () => void) {
    clearTimer();
    fullText = text;
    index = 0;
    onTick = tick;
    displayedText.value = '';
    isTyping.value = Boolean(fullText);

    if (!fullText) {
      isTyping.value = false;
      return;
    }

    timer = window.setInterval(() => {
      index += 1;
      displayedText.value = fullText.slice(0, index);
      onTick?.();

      if (index >= fullText.length) {
        finish();
      }
    }, speed);
  }

  function skip(text?: string) {
    if (text !== undefined) {
      fullText = text;
    }
    finish();
  }

  onUnmounted(clearTimer);

  return {
    displayedText,
    isTyping,
    start,
    skip,
  };
}
