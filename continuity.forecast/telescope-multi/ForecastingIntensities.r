forecast.period <- as.numeric(period)

# forecast data
foo <- telescope.forecast(tvp = intensities, horizon = forecast.period)
forecastValues <- as.numeric(foo$mean)