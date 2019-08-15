package skills.intTests.utils

import callStack.profiler.Profile
import groovy.util.logging.Slf4j

import java.nio.charset.StandardCharsets

@Slf4j
class SkillsService {

    WSHelper wsHelper

    SkillsService() {
        wsHelper = new WSHelper().init()
    }

    SkillsService(String username) {
        wsHelper = new WSHelper(username: username).init()
    }

    SkillsService(String username, String password) {
        wsHelper = new WSHelper(username: username, password: password).init()
    }

    SkillsService(String username, String password, String service) {
        wsHelper = new WSHelper(username: username, password: password, skillsService: service).init()
    }

    String getClientSecret(String projectId){
        wsHelper.get("/projects/${projectId}/clientSecret", "admin", null, false)
    }

    void setProxyCredentials(String clientId, String secretCode) {
        wsHelper.setProxyCredentials(clientId, secretCode)
    }

    def deleteAllMyProjects() {
        def projects = getProjects()
        projects.each {
            deleteProject(it.projectId)
            log.debug("Removed {} project", it.projectId)
        }
    }

    /**
     * high level utility to quickly construct rule-set schema
     * @param subjects list of subjects, where each item in the subject is props for a skill
     */
    def createSchema(List<List<Map>> subjects){
        String projId = subjects.first().first().projectId
        createProject([projectId: projId, name: projId])
        subjects.each {
            createSubject([projectId: projId, subjectId: it.first().subjectId, name: it.first().subjectId])
            it.each { Map params ->
                createSkill(params)
            }
        }
    }

    @Profile
    def createProject(Map props, String originalProjectId = null) {
        wsHelper.appPost(getProjectUrl(originalProjectId ?: props.projectId), props)
    }

    @Profile
    def moveProjectUp(Map props){
        wsHelper.adminPatch(getProjectUrl(props.projectId), '{"action": "DisplayOrderUp"}')
    }

    @Profile
    def moveProjectDown(Map props){
        wsHelper.adminPatch(getProjectUrl(props.projectId), '{"action": "DisplayOrderDown"}')
    }

    @Profile
    def updateProject(Map props, String oldProjectId = null) {
        wsHelper.adminPost(getProjectUrl( oldProjectId ?: props.projectId), props)
    }

    def projectIdExists(Map props){
        def id = URLEncoder.encode(props.projectId, 'UTF-8')
        wsHelper.appGet("/projectExist?projectId=${id}")
    }

    def projectNameExists(Map props){
        def name = URLEncoder.encode(props.projectName, 'UTF-8')
        wsHelper.appGet("/projectExist?projectName=${name}")
    }

    def getProjects() {
        wsHelper.appGet("/projects")
    }

    def getProject(String projectId) {
        wsHelper.adminGet(getProjectUrl(projectId))
    }

    def deleteProjectIfExist(String projectId) {
        Boolean res = wsHelper.appGet("/projectExist", [projectId: projectId])
        if(res ) {
            deleteProject(projectId)
        }
    }

    def deleteProject(String projectId) {
        wsHelper.adminDelete("/projects/${projectId}")
    }

    def deleteUserRole(String userId, String projectId, String role) {
        wsHelper.adminDelete("/projects/${projectId}/users/${userId}/roles/${role}")
    }

    @Profile
    def createSubject(Map props, boolean throwExceptionOnFailure = true) {
        wsHelper.adminPost(getSubjectUrl(props.projectId, props.subjectId), props, throwExceptionOnFailure)
    }

    @Profile
    def updateSubject(Map props, String oritinalSubjectId) {
        wsHelper.adminPost(getSubjectUrl(props.projectId, oritinalSubjectId), props)
    }

    def getSubjects(String projectId) {
        wsHelper.adminGet(getProjectUrl(projectId) + "/subjects")
    }

    def subjectNameExists(Map props){
        def subjName = URLEncoder.encode(props.subjectName, 'UTF-8')
        wsHelper.adminGet("/projects/${props.projectId}/subjectNameExists?subjectName=${subjName}")
    }

    def getSubjectDescriptions(String projectId, String subjectId) {
        String url = "/projects/${projectId}/subjects/${subjectId}/descriptions".toString()
        wsHelper.apiGet(url)
    }

    def getBadgeDescriptions(String projectId, String badgeId) {
        String url = "/projects/${projectId}/badges/${badgeId}/descriptions".toString()
        wsHelper.apiGet(url)
    }

    def badgeNameExists(Map props){
        def badgeName = URLEncoder.encode(props.badgeName, 'UTF-8')
        wsHelper.adminGet("/projects/${props.projectId}/badgeNameExists?badgeName=${badgeName}")
    }

    def skillNameExists(Map props){
        def skillName = URLEncoder.encode(props.skillName, 'UTF-8')
        wsHelper.adminGet("/projects/${props.projectId}/skillNameExists?skillName=${skillName}")
    }

    def deleteSubject(Map props) {
        wsHelper.adminDelete(getSubjectUrl(props.projectId, props.subjectId))
    }

    def createSkills(List<Map> props) {
        props.each {
            def res = createSkill(it)
            assert res.statusCode.value() == 200
        }
    }

    def shareSkill(String projectId, String skillId, String shareToProjectId){
        String url = "/projects/${projectId}/skills/${skillId}/shared/projects/${shareToProjectId}".toString()
        wsHelper.adminPost(url, null)
    }

    def deleteShared(String projectId, String skillId, String shareToProjectId) {
        String url = "/projects/${projectId}/skills/${skillId}/shared/projects/${shareToProjectId}"
        wsHelper.adminDelete(url)
    }

    def getSharedSkills(String projectId) {
        String url = "/projects/${projectId}/shared".toString()
        wsHelper.adminGet(url)
    }

    def getSharedWithMeSkills(String projectId) {
        String url = "/projects/${projectId}/sharedWithMe".toString()
        wsHelper.adminGet(url)
    }

    @Profile
    def createSkill(Map props, boolean throwExceptionOnFailure = true) {
        wsHelper.adminPost(getSkillUrl(props.projectId, props.subjectId, props.skillId), props, throwExceptionOnFailure)
    }

    def updateSkill(Map props, String originalSkillId) {
        wsHelper.adminPost(getSkillUrl(props.projectId, props.subjectId, originalSkillId ?: props.skillId), props)
    }

    def createBadge(Map props, String originalBadgeId = null) {
        wsHelper.adminPut(getBadgeUrl(props.projectId, originalBadgeId ?: props.badgeId), props)
    }

    def updateBadge(Map props, String originalBadgeId) {
        wsHelper.adminPut(getBadgeUrl(props.projectId, originalBadgeId ?: props.badgeId), props)
    }

    def assignDependency(Map props) {
        String url
        if(props.dependentProjectId){
            url = "/projects/${props.projectId}/skills/${props.skillId}/dependency/projects/${props.dependentProjectId}/skills/${props.dependentSkillId}"
        } else {
            url = "/projects/${props.projectId}/skills/${props.skillId}/dependency/${props.dependentSkillId}"
        }
        wsHelper.adminPost(url, props, false)
    }

    def deleteSkill(Map props) {
        wsHelper.adminDelete(getSkillUrl(props.projectId, props.subjectId, props.skillId), props)
    }
    def deleteSkillEvent(Map props) {
        wsHelper.adminDelete(getSkillUrl(props.projectId, null, props.skillEventId), props)
    }

    def getSkill(Map props) {
        wsHelper.adminGet(getSkillUrl(props.projectId, props.subjectId, props.skillId), props)
    }

    def getSkillsForSubject(String projectId, String subjectId) {
        wsHelper.adminGet("/projects/${projectId}/subjects/${subjectId}/skills")
    }

    def getSubject(Map props) {
        wsHelper.adminGet("/projects/${props.projectId}/subjects/${props.subjectId}")
    }

    def getBadge(String projectId, String badgeId) {
        this.getBadge([projectId: projectId, badgeId: badgeId])
    }
    def getBadge(Map props) {
        wsHelper.adminGet("/projects/${props.projectId}/badges/${props.badgeId}")
    }

    @Profile
    def addSkill(Map props, String userId = null, Date date = null) {
        if (userId) {
            assert date
            return wsHelper.apiPost("/projects/${props.projectId}/skills/${props.skillId}", [ userId : userId, timestamp:date.time])
        } else {
            return wsHelper.apiPut("/projects/${props.projectId}/skills/${props.skillId}", null)
        }
    }

    def addSkillAsProxy(Map props, String userId) {
        wsHelper.proxyApiPut(wsHelper.getTokenForUser(userId), "/projects/${props.projectId}/skills/${props.skillId}", null)
    }

    def getSkillSummaryAsProxy(String userId, String projId, String subjId=null, int version = -1) {
        String url = "/projects/${projId}/${subjId ? "subjects/${subjId}/" : ''}summary"
        if (version >= 0) {
            url += "&version=${version}"
        }
        wsHelper.proxyApiGet(wsHelper.getTokenForUser(userId), url)
    }

    def addBadge(Map props) {
        wsHelper.adminPut(getBadgeUrl(props.projectId, props.badgeId), props)
    }

    def assignSkillToBadge(String projectId, String badgeId, String skillId) {
        this.assignSkillToBadge([projectId: projectId, badgeId: badgeId, skillId: skillId])
    }

    def assignSkillToBadge(Map props) {
        wsHelper.adminPost(getAddSkillToBadgeUrl(props.projectId, props.badgeId, props.skillId), props)
    }

    def getSkillSummary(String userId, String projId, String subjId=null, int version = -1) {
        String url = "/projects/${projId}/${subjId ? "subjects/${subjId}/" : ''}summary?userId=${userId}"
        if (version >= 0) {
            url += "&version=${version}"
        }
        wsHelper.apiGet(url)
    }

    def getSingleSkillSummary(String userId, String projId, String skillId, int version = -1) {
        String url = "/projects/${projId}/skills/${skillId}/summary?userId=${userId}"
        if (version >= 0) {
            url += "&version=${version}"
        }
        wsHelper.apiGet(url)
    }

    def getCrossProjectSkillSummary(String userId, String projId, String otherProjectId, String skillId, int version = -1) {
        String url = "/projects/${projId}/projects/${otherProjectId}/skills/${skillId}/summary?userId=${userId}"
        if (version >= 0) {
            url += "&version=${version}"
        }
        wsHelper.apiGet(url)
    }

    def getBadgesSummary(String userId, String projId){
        String url = "/projects/${projId}/badges/summary?userId=${userId}"
        wsHelper.apiGet(url)
    }

    def getBadgeSummary(String userId, String projId, String badgeId, int version = -1){
        String url = "/projects/${projId}/badges/${badgeId}/summary?userId=${userId}"
        if (version >= 0) {
            url += "&version=${version}"
        }
        wsHelper.apiGet(url)
    }

    def listVersions(String projectId) {
        String url = "/projects/${projectId}/versions"
        wsHelper.appGet(url)
    }

    def getSkillDependencyInfo(String userId, String projId, String skillId, int version = -1) {
        String url = "/projects/${projId}/skills/${skillId}/dependencies?userId=${userId}"
        if (version >= 0) {
            url += "&version=${version}"
        }
        wsHelper.apiGet(url)
    }

    def getSkillsAvailableForDependency(String projId) {
        String url = "/projects/${projId}/dependency/availableSkills"
        wsHelper.adminGet(url)
    }

    def uploadIcon(Map props, File icon){
        Map body = [:]
        body.put("customIcon", icon)
        wsHelper.adminUpload("/projects/${props.projectId}/icons/upload", body)
    }

    def deleteIcon(Map props){
        wsHelper.adminDelete("/projects/${props.projectId}/icons/${props.filename}")
    }

    def getIcon(Map props){
        wsHelper.iconsGetImage("/icon/${props.projectId}/${props.filename}")
    }

    def getIconCssForProject(Map props){
        wsHelper.appGet("/projects/${props.projectId}/customIcons")
    }

    def getPerformedSkills(String userId, String project) {
        return wsHelper.adminGet("${getProjectUrl(project)}/performedSkills/${userId}?query=&limit=10&ascending=0&page=1&byColumn=0&orderBy=performedOn".toString())
    }

    def getUsersPerLevel(String projectId, String subjectId = null){
        String endpoint = subjectId ? "/projects/${projectId}/subjects/${subjectId}/rankDistribution/usersPerLevel" : "/projects/${projectId}/rankDistribution/usersPerLevel"
        return wsHelper.apiGet(endpoint)
    }

    def getRank(String userId, String projectId, String subjectId = null){
        String endpoint = subjectId ? "/projects/${projectId}/subjects/${subjectId}/rank" : "/projects/${projectId}/rank"
        endpoint = "${endpoint}?userId=${userId}"
        return wsHelper.apiGet(endpoint)
    }

    def getRankDistribution(String userId, String projectId, String subjectId = null){
        String endpoint = subjectId ? "/projects/${projectId}/subjects/${subjectId}/rankDistribution" : "/projects/${projectId}/rankDistribution"
        endpoint = "${endpoint}?userId=${userId}"
        return wsHelper.apiGet(endpoint)
    }

    def getPointHistory(String userId, String projectId, String subjectId=null, Integer version = -1){
        String endpointStart = subjectId ? getSubjectUrl(projectId, subjectId) : getProjectUrl(projectId)
        String url = "${endpointStart}/pointHistory?userId=${userId}"
        if (version >= 0) {
            url += "&version=${version}"
        }
        return wsHelper.apiGet(url.toString())
    }

    def getProjectUsers(String projectId, int limit = 10, int page = 1, String orderBy = 'userId', boolean ascending = true, String query = "") {
        return wsHelper.adminGet("${getProjectUrl(projectId)}/users?limit=${limit}&ascending=${ascending ? 1 : 0}&page=${page}&byColumn=0&orderBy=${orderBy}&query=${query}".toString())
    }

    def getSubjectUsers(String projectId, String subjectId, int limit = 10, int page = 1, String orderBy = 'userId', boolean ascending = true, String query = '') {
        return wsHelper.adminGet("${getSubjectUrl(projectId, subjectId)}/users?limit=${limit}&ascending=${ascending ? 1 : 0}&page=${page}&byColumn=0&orderBy=${orderBy}&query=${query}".toString())
    }

    def getUserStats(String projectId, String userId) {
        return wsHelper.adminGet("/projects/${projectId}/users/${userId}/stats".toString())
    }

    def getSkillUsers(String projectId, String skillId, int limit = 10, int page = 1, String orderBy = 'userId', boolean ascending = true, String query = '') {
        return wsHelper.adminGet("${getSkillUrl(projectId, null, skillId)}/users?limit=${limit}&ascending=${ascending ? 1 : 0}&page=${page}&byColumn=0&orderBy=${orderBy}&query=${query}".toString())
    }

    def getBadgeUsers(String projectId, String badgeId, int limit = 10, int page = 1, String orderBy = 'userId', boolean ascending = true, String query = '') {
        return wsHelper.adminGet("${getBadgeUrl(projectId, badgeId)}/users?limit=${limit}&ascending=${ascending ? 1 : 0}&page=${page}&byColumn=0&orderBy=${orderBy}&query=${query}".toString())
    }

    def getLevels(String projectId, String subjectId = null) {
        return wsHelper.adminGet(getLevelsUrl(projectId, subjectId))
    }

    def adminGetUserLevelForProject(String projectId, String userId){
        return wsHelper.apiGet(getUserLevelForProjectUrl(projectId, userId))
    }

    def apiGetUserLevelForProject(String projectId, String userId){
        return wsHelper.apiGet(getUserLevelForProjectUrl(projectId, userId))
    }

    def editLevel(String projectId, String subjectId, String level, Map props){
        return wsHelper.adminPost(getEditLevelUrl(projectId, subjectId, level), props)
    }

    def addLevel(String projectId, String subjectId, Map props) {
        return wsHelper.adminPost(getAddLevelUrl(projectId, subjectId), props)
    }

    def deleteLevel(String projectId, String subjectId){
        return wsHelper.adminDelete(getDeleteLevelUrl(projectId, subjectId))
    }

    def getMetricsChart(String projectId, chartBuilderId, String section, String sectionId, Map props=null) {
        String endpoint = "/projects/${projectId}/${section}/${sectionId}/metrics/${chartBuilderId}"
        wsHelper.adminGet(endpoint, props)
    }

    def getAllMetricsChartsForSection(String projectId, String section, String sectionId, Map props=null) {
        String endpoint = "/projects/${projectId}/${section}/${sectionId}/metrics/"
        wsHelper.adminGet(endpoint, props)
    }

    def getSetting(String projectId, String setting){
        return wsHelper.adminGet(getSettingUrl(projectId, setting))
    }

    def getSettings(String project){
        return wsHelper.adminGet(getSettingsUrl(project))
    }

    def getPublicSetting(String setting, String settingGroup){
        return wsHelper.appGet("/public/settings/${setting}/group/${settingGroup}")
    }

    def getPublicSettings(String settingGroup){
        return wsHelper.appGet("/public/settings/group/${settingGroup}")
    }

    def findLatestSkillVersion(String projectId) {
        return wsHelper.adminGet("${getProjectUrl(projectId)}/latestVersion")
    }

    def getRootUsers() {
        return wsHelper.rootGet('/rootUsers')
    }

    def getNonRootUsers(String query) {
        return wsHelper.rootGet("/users/${query}")
    }

    def getCurrentUser() {
        return wsHelper.appGet("/userInfo")
    }

    def updateUserInfo(Map userInfo) {
        return wsHelper.appPost('/userInfo', userInfo)
    }

    def isRoot() {
        return wsHelper.rootGet('/isRoot')
    }

    def addRootRole(String userId) {
        return wsHelper.rootPut("/addRoot/${userId}")
    }

    def removeRootRole(String userId) {
        return wsHelper.delete("/deleteRoot/${userId}", 'root', null)
    }

    def createRootAccount(Map<String, String> userInfo) {
        return wsHelper.createRootAccount(userInfo)
    }

    def grantRoot() {
        return wsHelper.grantRoot()
    }

    def addOrUpdateGlobalSetting(String setting, Map value) {
        return wsHelper.rootPut("/global/settings/${setting}", value)
    }

    def changeSetting(String project, String setting, Map value){
        return wsHelper.adminPost(getSettingUrl(project, setting), value)
    }

    def changeSettings(String project, List<Map> settings){
        return wsHelper.adminPost(getSettingsUrl(project), settings)
    }

    def checkSettingsValidity(String project, List<Map> settings){
        return wsHelper.adminPost("${getProjectUrl(project)}/settings/checkValidity".toString(), settings)
    }

    def addProjectAdmin(String projectId, String userId) {
        return wsHelper.adminPut(getAddProjectAdminUrl(projectId, userId))
    }

    boolean doesUserExist(String username) {
        return wsHelper.rootContextGet("/userExists/${username}")
    }

    boolean doesSubjectNameExist(String projectId, String subjectName) {
        String encoded = URLEncoder.encode(subjectName, StandardCharsets.UTF_8.toString())
        return wsHelper.adminGet("/projects/${projectId}/subjectNameExists?subjectName=${encoded}")
    }

    boolean doesBadgeNameExist(String projectId, String subjectName) {
        String encoded = URLEncoder.encode(subjectName, StandardCharsets.UTF_8.toString())
        return wsHelper.adminGet("/projects/${projectId}/badgeNameExists?badgeName=${encoded}")
    }

    boolean doesSkillNameExist(String projectId, String skillName) {
        String encoded = URLEncoder.encode(skillName, StandardCharsets.UTF_8.toString())
        return wsHelper.adminGet("/projects/${projectId}/skillNameExists?skillName=${encoded}")
    }

    boolean doesEntityExist(String projectId, String id) {
        return wsHelper.adminGet("/projects/${projectId}/entityIdExists?id=${id}")
    }

    private String getProjectUrl(String project) {
        return "/projects/${project}".toString()
    }

    private String getSubjectUrl(String project, String subject) {
        return "${getProjectUrl(project)}/subjects/${subject}".toString()
    }

    private String getSkillUrl(String project, String subject, String skill) {
        if (subject) {
            return "${getSubjectUrl(project, subject)}/skills/${skill}".toString()
        } else {
            return "${getProjectUrl(project)}/skills/${skill}".toString()
        }
    }

    private String getSkillEventUrl(String project, String skill) {
        // /projects/{projectId}/skills/{skillEventId}
        return "${getProjectUrl(project)}/skills/${skill}".toString()
    }

    private String getBadgeUrl(String project, String badge) {
        return "${getProjectUrl(project)}/badges/${badge}".toString()
    }

    private String getAddSkillToBadgeUrl(String project, String badge, String skillId) {
        return "${getProjectUrl(project)}/badge/${badge}/skills/${skillId}".toString()
    }

    private String getSaveIconUrl(String project){
        return "/icons/upload/${project}"
    }

    private String getLevelsUrl(String project, String subject){
        if(subject){
            return "${getSubjectUrl(project, subject)}/levels".toString()
        }else {
            return "${getProjectUrl(project)}/levels".toString()
        }
    }

    private String getUserLevelForProjectUrl(String project, String userId){
        return "${getProjectUrl(project)}/level?userId=${userId}".toString()
    }

    private String getDeleteLevelUrl(String project, String subject){
        return "${getLevelsUrl(project, subject)}/last".toString()
    }

    private String getAddLevelUrl(String project, String subject){
        return "${getLevelsUrl(project, subject)}/next".toString()
    }

    private String getEditLevelUrl(String project, String subject, String level){
        return "${getLevelsUrl(project, subject)}/edit/${level}".toString()
    }

    private String getSettingsUrl(String project){
        return "${getProjectUrl(project)}/settings"
    }

    private String getSettingUrl(String project, String setting){
        return "${getProjectUrl(project)}/settings/${setting}"
    }

    private String getAddProjectAdminUrl(String project, String userId) {
        return "/projects/${project}/users/${userId}/roles/ROLE_PROJECT_ADMIN"
    }
}
