title: DevOps实践：Gitlab、Jenkins
date: 2019-03-20 17:31:38
category: work
tags:
  - devops
  - gitlab
  - jenkins
  - jenkins-pipeline
  - jenkinsfile
---

Wiki百科上对**DevOps**一词的解释：DevOps（Development和Operations的组合詞）是一种重视「软件开发人员（Dev）」和「IT运维技术人员（Ops）」之间沟通合作的文化、运动或慣例。 透过自动化「软件交付」和「架构变更」的流程，来使得构建、测试、发布软件能够更加地快捷、频繁和可靠。

而Gitlab和Jenkins是我们在DevOps中常用的工具，本文将简单介绍下怎样搭配Gitlab与Jenkins来起步DevOps。

## Gitlab

### 生成Access Token

访问 *User Settings > Access Token*，生成**Personal Access Tokens**。如下图：

![](/img/gitlab/profile-access-token.png)

点击`Create personal access token`生成访问令牌，并记住它。之后配置Jenkins时需要。


## 创建一个Jenkins pipeline作业

### 配置Jenkins的Gitlab访问凭据

进入Jenkins的全局设置，配置Gitlab。分别设置`Connection name`、`Gitlab host URL`和`Credentials`。

点击`Credentials`右侧的`Add`按钮，添加`Gitlab API token`。在弹框里选择`Gitlab API token`类型，并设置`API token`。

![](/img/jenkins/jenkins-credentials.png)

`API token`为之前从Gitlab里生成的**Access Token**。

### 创建Job

创建一个流水线（pipeline）风格的作业（Job）。

![](/img/jenkins/job-new-pipeline.png)

#### Build Triggers

构建触发规则选中`Build when a change is pushed to Gitlab. Gitlab webhook URL: <jenkins job url>`。`Advanced...`按钮，并在`Secret token`选项点击`Generate`生成**Secret token**（记住这个token，在配置Gitlab仓库时需要）。

#### Pipeline

1. **Definition**选择`Pipeline script from SCM`
2. **SCM**选择`Git`
    - `Repository URL`填写需要Jenkins自动化构建的Git仓库
    - `Credentials`选择已定义的一个凭据或者新建一个
    - `Branches to build`请填写自己希望构建的分支
    - `Script Path`填写相对Git仓库根目录的**Jenkinsfile**路径，这里我们设置为`Jenkinsfile.develop`
3. 点击**Save**按钮保存

### 配置Gitlab代码Push时自动通知Jenkins

进入Gitlab某个项目，从左侧菜单选择`Settings > Integrations`。

1. **URL**：填写Jenkins作业地址，如：`http://<ip>:<port>/project/<job name>`
2. **Secret Token**：填写在作罢Jenkins作业**Build Triggers**步骤时获得的token
3. **Push events**：选中`Push events`（酌情可以指定具体的分支名）和`Merge request events`。

最后点击`Save chagens`按钮保存设置，也可以在保存前先点击`Test`按钮测试下配置是否正确。`也许你需要去掉**Enable SSL verification**，这看你的具体情况设置`。

### 示例Jenkinsfile

这里有一个示例的**Jenkinsfile**配置文件，定义了3个阶段，分别是：

1. **Test**：执行单元测试，测试完成后收集测试结果。可以在pipeline在**Test**界面查看测试报告。
2. 

``` Jenkinsfile
pipeline {
    agent any
    options {
        timeout(time: 2, unit: 'HOURS')
    }
    stages  {
        stage('Test') {
            steps {
                sh './sbt test'
            }
            post {
                always {
                    junit '**/target/test-reports/*.xml'
                }
            }
        }
        stage('Package') {
            steps {
                sh './sbt "project hongka-resource-app" assembly "project hongka-resource-converter-app" assembly'
            }
        }
        stage('Deliver for develop') {
            when {
                expression {
                    currentBuild.result == null || currentBuild.result == 'SUCCESS'
                }
            }
            environment {
                DEPLOY_CREDENTIALS = credentials('develop_username_password')
            }
            steps {
                sh "./scripts/deploy_develop.sh $DEPLOY_CREDENTIALS_USR $DEPLOY_CREDENTIALS_PSW"
            }
        }
    }
    post {
        always {
            echo 'This will always run'
        }
        success {
            archiveArtifacts artifacts: '**/target/scala-2.12/*.jar', fingerprint: true
        }
        failure {
            echo 'This will run only if failed'
        }
        unstable {
            echo 'This will run only if the run was marked as unstable'
        }
        changed {
            echo 'This will run only if the state of the Pipeline has changed'
            echo 'For example, if the Pipeline was previously failing but is now successful'
        }
    }
}
```

## 思考

1. 这里没有选择Jenkins的多分支pipeline构建，而是使用单分支pipeline构建。考虑主要如下：
    1. 通常仓库里都会有一些不属于开发、测试的分支及临时分支存在，不希望每次这些分支被Push时都会触发自动编译
    2. 多分支构建都使用同一个Jenkinsfile，会造成Jenkinsfile内容变多且复杂
    3. 我更喜欢对不同的分支建立不同的Jenkins作业，这样从管理上，特别是权限控制上更方便
2. 因为使用了单分支pipeline，所以需要为不同的构建环境创建不同的**Jenkinsfile**配置文件。这里有一个推荐：
    - `scripts/Jenkinsfile.develop`：开发环境
    - `scripts/Jenkinsfile.test`：测试环境
    - `scripts/Jenkinsfile.production`：所产环境
    - `scripts/Jenkinsfile.master`：演示环境（可选）



