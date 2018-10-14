forecast.period <- as.numeric(period)

# forecast data
foo <- telescope.forecast(tvp = intensities, horizon = forecast.period, hist.covar = hist.covar.matrix, future.covar = future.covar.matrix)
forecastValues <- as.numeric(foo$mean)