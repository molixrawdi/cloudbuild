## add githubactions

```
# Window
choco install act-cli

# MacOS
brew install act

# Linux
curl https://raw.githubusercontent.com/nektos/act/master/install.sh | sudo bash

```

### act file '~/.actrc'

```
# .actrc
-P ubuntu-latest=node:16-buster-slim
-P ubuntu-22.04=node:16-bullseye-slim
-P ubuntu-20.04=node:16-buster-slim
-P ubuntu-18.04=node:16-buster-slim
```

### usage examples

```
act pull_request
act -l # print all available jobs inside .github/workflows
act --job <jon-name> ## runs that job
act --job 'show' run a specific show job
act --graph ### draws the available workflow jobs in the term

```

### job / workflow example

```
# .github/workflows/test.yml

name: Convert files into Zip folder

on: pull_request

jobs:
  show:
    runs-on: ubuntu-latest
    steps:
      - name: Show Env
        run: echo "Env ${{ env.ENV_ID }}"
```

### envfile

```
act --env-file=my-custom.env
```


### example

```
# .github/workflows/test.yml

name: Learn environment secrets 

on: pull_request

jobs:
  show:
    runs-on: ubuntu-latest
    steps:
      - name: Show env
        run: echo "App SECRET ${{ secrets.APP_SECRET }}"
      - name: Show varibale
        run: echo "App ID ${{ secrets.APP_ID }}"
```