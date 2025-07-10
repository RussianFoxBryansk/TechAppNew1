package com.example.techappnew.network

import com.example.techappnew.*
import com.example.techappnew.mask.Cumpus
import com.example.techappnew.mask.Device
import com.example.techappnew.mask.Place
import com.example.techappnew.mask.Problem
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface UserApi {
    @GET("users/{id}")
    suspend fun getUser (@Path("id") userId: Int): User

    @POST("users/auth")
    suspend fun loginUser(@Body loginRequest: LoginRequest): Response<LoginPOST>

    @GET("users/login/{login}")
    suspend fun getUserByLogin(@Path("login") login: String): Response<User>

    @POST("users/")
    suspend fun regUser (@Body addRequest: AddRequest): User
    @POST("users/send-password/{email}")
    suspend fun sendPasswor(@Path("email") email: String): Response<Unit>
}
interface CumpusApi {
    @GET("campus/{campus_id}")
    suspend fun getCumpusId(@Path("campus_id") cumpusId: Int): Cumpus

    @GET("campus/")
    suspend fun getCumpusList(): Response<List<Cumpus>>

    @POST("campus/")
    suspend fun createCampus(@Body request: CumpusRequest): Response<Unit>

    @DELETE("campus/{campus_id}")
    suspend fun deleteCampus(@Path("campus_id") campusId: Int): Response<Unit>
}

interface RoomsApi {
    @GET("classrooms/")
    suspend fun getAllRooms(): Response<List<ClassroomRequest>>

    @POST("classrooms/")
    suspend fun createClassroom(@Body request: ClassroomRequest): Response<Unit>

    @DELETE("classrooms/{classroom_id}")
    suspend fun deleteClassroom(@Path("classroom_id") classroomId: Int): Response<Unit>
}
interface PlacesApi {
    @GET("places/")
    suspend fun getPlaces(@Query("classroom_id") classroomId: Int): Response<List<Place>>

    @POST("places/")
    suspend fun createPlace(@Body place: PlaceRequest): Response<Place>

    @DELETE("places/{place_id}")
    suspend fun deletePlace(@Path("place_id") placeId: Int): Response<Unit>
}
interface DevicesApi {
    @POST("devices/")
    suspend fun createDevice(@Body device: DeviceRequest): Response<Device>

    @PUT("devices/{device_id}")
    suspend fun updateDevice(
        @Path("device_id") deviceId: Int,
        @Body device: DeviceRequest
    ): Response<Device>

    @DELETE("devices/{device_id}")
    suspend fun deleteDevice(@Path("device_id") deviceId: Int): Response<Unit>
    @GET("devices/{device_id}")
    suspend fun getDeviceById(@Path("device_id") deviceId: Int): Response<Device>

    @GET("devices/place/{place_id}")
    suspend fun getDevicesByPlace(@Path("place_id") placeId: Int): Response<List<Device>>
}
interface ProblemsApi {
    @GET("problems/")
    suspend fun getProblems(
        @Query("device_id") deviceId: Int? = null,
        @Query("user_id") userId: Int? = null
    ): Response<List<Problem>>

    @POST("problems/")
    suspend fun createProblem(@Body problemRequest: ProblemRequest): Response<Problem>

    @DELETE("problems/{problem_id}")
    suspend fun deleteProblem(@Path("problem_id") problemId: Int): Response<Unit>

    @PUT("problems/{problem_id}")
    suspend fun updateProblem(
        @Path("problem_id") problemId: Int,
        @Body updateRequest: ProblemUpdateRequest
    ): Response<Problem>
}
