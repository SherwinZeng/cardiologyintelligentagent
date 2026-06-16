<script setup lang="ts">
const beatWidth = 280
const beatsPerTile = 4
const tileWidth = beatWidth * beatsPerTile
/** 每搏动描迹耗时（4 搏 ≈ 24s 扫完整段） */
const beatDuration = '6s'
const sweepHead = 88

function buildBeatSegment(startX: number, withMove: boolean) {
  const x = startX
  const parts = withMove ? [`M${x} 60`, `H${x + 28}`] : [`H${x + 28}`]

  parts.push(
    `C${x + 34} 60 ${x + 38} 54 ${x + 44} 46`,
    `C${x + 50} 42 ${x + 54} 46 ${x + 58} 54`,
    `C${x + 62} 60 ${x + 66} 60 ${x + 72} 60`,
    `H${x + 82}`,
    `L${x + 85} 70 L${x + 89} 62 L${x + 93} 4 L${x + 97} 98 L${x + 101} 62 L${x + 105} 60`,
    `H${x + 112}`,
    `C${x + 118} 60 ${x + 122} 52 ${x + 128} 44`,
    `C${x + 134} 38 ${x + 140} 42 ${x + 146} 48`,
    `C${x + 152} 54 ${x + 158} 60 ${x + 165} 60`,
    `H${x + 280} 60`,
  )

  return parts.join(' ')
}

const ecgPath = [
  buildBeatSegment(0, true),
  ...Array.from({ length: beatsPerTile - 1 }, (_, index) =>
    buildBeatSegment((index + 1) * beatWidth, false),
  ),
].join(' ')
</script>

<template>
  <div class="welcome-ecg" aria-hidden="true">
    <svg
      class="welcome-ecg__svg"
      :viewBox="`0 0 ${tileWidth} 120`"
      preserveAspectRatio="none"
    >
      <path class="welcome-ecg__rail" :d="ecgPath" />
      <path
        class="welcome-ecg__beam"
        :d="ecgPath"
        :pathLength="tileWidth"
        :style="{
          strokeDasharray: `${sweepHead} ${beatWidth - sweepHead}`,
        }"
      >
        <animate
          attributeName="stroke-dashoffset"
          from="0"
          :to="String(-beatWidth)"
          :dur="beatDuration"
          repeatCount="indefinite"
        />
      </path>
    </svg>
  </div>
</template>

<style scoped lang="scss">
@use '../styles/welcome-ecg-wave.scss';
</style>
