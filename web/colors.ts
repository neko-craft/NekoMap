export default (temperature: number, rainfall: number, type: 0 | 1 | 2) => {
  temperature = clamp(temperature, 0, 1)
  rainfall = clamp(rainfall, 0, 1) * temperature

  const corners = [
    [191, 183, 85],
    [128, 180, 151],
    [71, 205, 51]
  ]
  const a = temperature - rainfall, b = 1 - temperature

  return (clamp(a * corners[0][0] + b * corners[1][0] + rainfall * corners[2][0], 0, 255) << 16) |
    (clamp(a * corners[0][1] + b * corners[1][1] + rainfall * corners[2][1], 0, 255) << 8) |
    clamp(a * corners[0][2] + b * corners[1][2] + rainfall * corners[2][2], 0, 255)
}

function clamp (value: number, min: number, max: number) {
  if (value > max) return max
  else if (value < min) return min
  else return value
}