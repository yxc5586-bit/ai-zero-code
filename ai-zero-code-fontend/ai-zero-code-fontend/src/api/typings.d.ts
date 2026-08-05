declare namespace API {
  type BaseResponse<T> = {
    code?: number
    data?: T
    message?: string
  }

  type AppAddRequest = { initPrompt?: string }
  type AppAdminUpdateRequest = { id?: string; appName?: string; cover?: string; priority?: number }
  type AppDeployRequest = { appId?: string }
  type AppUpdateRequest = { id?: string; appName?: string }
  type DeleteRequest = { id?: string }

  type AppQueryRequest = {
    pageNum?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    id?: string
    appName?: string
    cover?: string
    initPrompt?: string
    codeGenType?: string
    deployKey?: string
    priority?: number
    userId?: string
  }

  type AppVO = {
    id?: string
    appName?: string
    cover?: string
    initPrompt?: string
    codeGenType?: string
    deployKey?: string
    deployedTime?: string
    priority?: number
    userId?: string
    editTime?: string
    createTime?: string
    updateTime?: string
    createUser?: UserVO
  }

  type PageAppVO = {
    records?: AppVO[]
    pageNumber?: string
    pageSize?: string
    totalPage?: string
    totalRow?: string
    optimizeCountQuery?: boolean
  }

  type User = {
    id?: string
    userAccount?: string
    userPassword?: string
    userName?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
    editTime?: string
    createTime?: string
    updateTime?: string
    isDelete?: number
  }

  type UserVO = {
    id?: string
    userAccount?: string
    userName?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
    createTime?: string
  }

  type LoginUserVO = UserVO & { updateTime?: string }
  type UserAddRequest = {
    userName?: string
    userAccount?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
  }
  type UserLoginRequest = { userAccount?: string; userPassword?: string }
  type UserRegisterRequest = {
    userAccount?: string
    userPassword?: string
    checkPassword?: string
  }
  type UserUpdateRequest = {
    id?: string
    userName?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
  }
  type UserQueryRequest = {
    pageNum?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    id?: string
    userName?: string
    userAccount?: string
    userProfile?: string
    userRole?: string
  }
  type PageUserVO = {
    records?: UserVO[]
    pageNumber?: string
    pageSize?: string
    totalPage?: string
    totalRow?: string
    optimizeCountQuery?: boolean
  }

  type BaseResponseAppVO = BaseResponse<AppVO>
  type BaseResponseBoolean = BaseResponse<boolean>
  type BaseResponseLoginUserVO = BaseResponse<LoginUserVO>
  type BaseResponseLong = BaseResponse<string>
  type BaseResponsePageAppVO = BaseResponse<PageAppVO>
  type BaseResponsePageUserVO = BaseResponse<PageUserVO>
  type BaseResponseString = BaseResponse<string>
  type BaseResponseUser = BaseResponse<User>
  type BaseResponseUserVO = BaseResponse<UserVO>
  type ServerSentEventString = true

  type chatToGenCodeParams = { appId: string; message: string }
  type getAppVOByIdByAdminParams = { id: string }
  type getAppVOByIdParams = { id: string }
  type getUserByIdParams = { id: string }
  type getUserVOByIdParams = { id: string }
}
