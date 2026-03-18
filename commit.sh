#!/bin/bash
cd /home/eren/ALEX
git add -A
git commit -m "Fix D-pad navigation: sync focus with scroll animation

- Block navigation while scroll is animating to prevent focus ring jumping ahead
- Add isScrolling() check to SmoothScroller
- Reduce hold throttle to 120ms (was 180ms) for better responsiveness
- Increase hero debounce to 400ms to reduce updates during rapid navigation
- Match CSS transition timing (180ms) with scroll animation speed
- Add CSS containment to cards to isolate repaints
- Use cubic-bezier easing for smoother feel"
git push
