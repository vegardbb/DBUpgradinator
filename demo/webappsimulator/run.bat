@echo on
: Run index program, where the function partOne is not declared as async
for /l %%x in (1, 1, 4000) do (
    echo Round %%x
    node --max_old_space_size=5000 index.js
    timeout /t 1 /nobreak
)
