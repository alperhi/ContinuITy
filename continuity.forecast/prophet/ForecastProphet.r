library(prophet)

forecast.period <- as.numeric(period)

df = data.frame(ds=dates, y=intensities)
m <- prophet(df)

future <- make_future_dataframe(m, periods = forecast.period, freq = 60 * 60)
forecast <- predict(m, future)

forecastValues <- tail(forecast[['yhat']], forecast.period)
