UPDATE sequentialnumbercounter SET key = substring(key, 12, (length(key)-12)) WHERE key LIKE 'SEQUENTIAL(%)';
