// Shared Daily Pipe level used across the promos (6 x 7 cells).
window.LEVEL = {
  cols: 6, rows: 7,
  blocked: [[5, 0], [4, 1], [0, 3], [5, 2], [0, 5], [2, 6]],
  // a clean route from the valve to the checkered finish
  route: [[0, 0], [1, 0], [2, 0], [3, 0], [3, 1], [3, 2], [2, 2], [1, 2], [1, 3], [1, 4], [2, 4], [3, 4], [4, 4], [4, 5], [4, 6], [5, 6]],
  // a better route with a cross (second pass = +50)
  crossRoute: [[0, 0], [1, 0], [2, 0], [2, 1], [2, 2], [2, 3], [3, 3], [4, 3], [4, 2], [3, 2], [2, 2], [1, 2], [1, 3], [1, 4], [2, 4], [3, 4], [4, 4], [4, 5], [4, 6], [5, 6]],
  // a route that runs into the board edge and leaks
  leakRoute: [[0, 0], [1, 0], [2, 0], [3, 0], [3, 1], [3, 2], [3, 3], [4, 3], [5, 3]],
};
// time helpers for a route: place pieces from tPlace every `gap`, flow from tFlow at `speed`
window.schedule = (route, tPlace, gap, tFlow, speed, finished = true) => {
  const n = route.length, placeT = [];
  const seen = new Set();
  let k = 0;
  for (let i = 0; i < n; i++) {
    const key = route[i].join(',');
    if (i === 0 || (finished && i === n - 1)) { placeT.push(undefined); continue; }
    if (seen.has(key)) { placeT.push(undefined); continue; }
    seen.add(key); placeT.push(tPlace + k * gap); k++;
  }
  const end = tFlow + n / speed;
  return { placeT, tFlow, speed, end, count: k, places: placeT.filter((x) => x != null) };
};
