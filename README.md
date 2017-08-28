## Overview

The goal of this exercise is to demo how to use AWS [SAM](https://aws.amazon.com/about-aws/whats-new/2016/11/introducing-the-aws-serverless-application-model/) and Cognito to provide a secured service backed by [ElasticSearch](https://aws.amazon.com/elasticsearch-service/).

There are many examples for writing SAM based on NodeJS, but not for Java. However, after finish the exercise, I feel like using Java for AWS is not as bad as I expect. 

## Requirement

* Provide a endpoint on API Gateway for searching, /search
* Provide a endpoint on API Gateway for get identity token, /auth
* The /search endpoint should be only available to authenticated user

## Implementation Detail

### Architecture

![Architecture](/images/es_lambda_aws_java.jpg)

### Setup AWS

#### Elasticsearch

 Create a new domain, with index *example*, and then index the sample documents into the type *plan*.
 
  * The index name is *example"
  * The type is *plan*
  * The source data is in *data* folder, which include a layout and shorten sample.csv
  * The Java code for index the document is EsInitializer
  * The DataTest demos how to indexing
  * The domain has 2 m4 nodes, EBS using 10GB SSD
  
#### Cognito

 Setup user pool
 
  * The pool: alex-pool
  * In *App Clients* tab, create a new app client, without app client secret
  * In *Uses and groups* tab, create a new user alex, and assign the temporary password, make sure you set the email, otherwise you will not be able to activate
  * You can activate the user by using aws cli. Below is an example.
  
```bash
aws cognito-idp admin-create-user --user-pool-id us-east-1_RFm2UEkpf --username alex --user-attributes 'Name=email,Value=alex.li@yahoo.com' --temporary-password 'TPa$$w0rd'
{
    "User": {
        "Username": "alex",
        "Enabled": true,
        "UserStatus": "FORCE_CHANGE_PASSWORD",
        "UserCreateDate": 1503844026.875,
        "UserLastModifiedDate": 1503844026.875,
        "Attributes": [
            {
                "Name": "sub",
                "Value": "b1bba24f-4910-4660-b800-59f6cf0db609"
            },
            {
                "Name": "email",
                "Value": "alex.li@yahoo.com"
            }
        ]
    }
}

aws cognito-idp admin-initiate-auth --user-pool-id us-east-1_RFm2UEkpf --client-id 7mjuj4d9mjf96kadvb2ok0foo6 --auth-flow ADMIN_NO_SRP_AUTH --auth-parameters 'USERNAME=alex,PASSWORD=TPa$$w0rd' --region us-east-1
{
    "ChallengeName": "NEW_PASSWORD_REQUIRED",
    "ChallengeParameters": {
        "USER_ID_FOR_SRP": "alex",
        "requiredAttributes": "[]",
        "userAttributes": "{\"email\":\"alex.li@yahoo.com\"}"
    },
    "Session": "lJ5KWtNIJTqlu1hhVOSMrctI3PIpNU6kRu0HeCKipkXpvbJb-BAAXivL7QmIALJt6BQzdhYd920FXgBJgFioM4IvQ-olgkcl-siUAf1HVSXO9SvkqB_Cl2vQgVa1PFqWVDNmKgDZxgmm1Znw_023DYN93T1AWbRnFZP2dphgKUqPPFTwF67uWj-XqmSwavct4Hpq3DGzspCCqilQNEc5lHJSCM_0VwDVVAmPv8OKQsk-QX0IFIxuq491Mr0YI82TO3yQ99uZVUp4dyyzV7pL7MORchUX7W13G7bP86cGoPp3i0HKluOQTREcR_6CzCbZaZhNAf6V-4J1H4TZiQtZ2VFQDUaWvZkBF4cW2WVLUQ0Xx3PUzU00HUW1w1IZskcdxxY_3OWJjdm4sWvjCxm8C1rGtX09kDfNsezQK05t5LblQMyyPo5XGLbUiPdR1wNUouxT5phUrlFfYUOS_bqqnk-TgiC4z9l6gdFKUbzojPuPP_0eYRepGPwy3g0x1e2suOFKM5ojR3yCWiSX1Qx1ef7iT0vLzWs6ufICrpuArDozpllwF5wqlE8LcDKZ2gAmoCzMMIumraoro2NsxBzdZWjTQUtr572XweanhV46P-li9JUEOB0ULVcLCU1PcI84-SUx1UoGc5bLngo_-654mxOyNUZ9Qlw1hfGiu-uY9j2ZE8UHfS6IRFswcOzeWtho"
}

aws cognito-idp admin-respond-to-auth-challenge --user-pool-id us-east-1_RFm2UEkpf --client-id 7mjuj4d9mjf96kadvb2ok0foo6 --challenge-name NEW_PASSWORD_REQUIRED --challenge-responses 'NEW_PASSWORD=Pa$$w0rd,USERNAME=alex' --session "lJ5KWtNIJTqlu1hhVOSMrctI3PIpNU6kRu0HeCKipkXpvbJb-BAAXivL7QmIALJt6BQzdhYd920FXgBJgFioM4IvQ-olgkcl-siUAf1HVSXO9SvkqB_Cl2vQgVa1PFqWVDNmKgDZxgmm1Znw_023DYN93T1AWbRnFZP2dphgKUqPPFTwF67uWj-XqmSwavct4Hpq3DGzspCCqilQNEc5lHJSCM_0VwDVVAmPv8OKQsk-QX0IFIxuq491Mr0YI82TO3yQ99uZVUp4dyyzV7pL7MORchUX7W13G7bP86cGoPp3i0HKluOQTREcR_6CzCbZaZhNAf6V-4J1H4TZiQtZ2VFQDUaWvZkBF4cW2WVLUQ0Xx3PUzU00HUW1w1IZskcdxxY_3OWJjdm4sWvjCxm8C1rGtX09kDfNsezQK05t5LblQMyyPo5XGLbUiPdR1wNUouxT5phUrlFfYUOS_bqqnk-TgiC4z9l6gdFKUbzojPuPP_0eYRepGPwy3g0x1e2suOFKM5ojR3yCWiSX1Qx1ef7iT0vLzWs6ufICrpuArDozpllwF5wqlE8LcDKZ2gAmoCzMMIumraoro2NsxBzdZWjTQUtr572XweanhV46P-li9JUEOB0ULVcLCU1PcI84-SUx1UoGc5bLngo_-654mxOyNUZ9Qlw1hfGiu-uY9j2ZE8UHfS6IRFswcOzeWtho"
{
    "AuthenticationResult": {
        "ExpiresIn": 3600,
        "IdToken": "eyJraWQiOiJESW9cL093QVF4NXYwTjJvTEM5azNzeXpUUllOa2RiMGZDbmJ3T1htZFNhbz0iLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJiMWJiYTI0Zi00OTEwLTQ2NjAtYjgwMC01OWY2Y2YwZGI2MDkiLCJhdWQiOiI3bWp1ajRkOW1qZjk2a2FkdmIyb2swZm9vNiIsInRva2VuX3VzZSI6ImlkIiwiYXV0aF90aW1lIjoxNTAzODQ0MDc5LCJpc3MiOiJodHRwczpcL1wvY29nbml0by1pZHAudXMtZWFzdC0xLmFtYXpvbmF3cy5jb21cL3VzLWVhc3QtMV9SRm0yVUVrcGYiLCJjb2duaXRvOnVzZXJuYW1lIjoiYWxleDIiLCJleHAiOjE1MDM4NDc2NzksImlhdCI6MTUwMzg0NDA3OSwiZW1haWwiOiJhbGV4LmxpQHBydWRlbnRpYWwuY29tIn0.GmQH5PGw1Bxddtz2I60HM4XTT6p2SmvtJ2-QSvDSNtkqFY9BNvLXCvsqrh2x7Sawgk7KUW6kysRHcVfXtn_NDF7pyXT80UILlBm17hMJFTtv_oioDyzcWMfrXCiKGsPLlwT713BpCnNZYd3VvYYF0Tn_vQeEasTHjSDr-XGA0e10c6KrVLV3kZrquHAZn8yRo82zMeT9hacjLYrzh2q7gkIWz4aE8Gc7ZdrXr3Koe0xRuvCquUavt-eqq4PKLO7gfmArw9ysZzq3F_i2b-gC4mWHXLRbeN6BHliVQXS37iZhLVVlAK-5GrPPE1f74PAOe6oOdNVH89sEps_pR1M3IQ",
        "RefreshToken": "eyJjdHkiOiJKV1QiLCJlbmMiOiJBMjU2R0NNIiwiYWxnIjoiUlNBLU9BRVAifQ.NEGwWxnjPpcq9v5_zJsUxJ2IP-C0idbOq3b1jwGTj09EkqCb1n4YEa0lIwPOFEUcqFVkEvkH6yRU7x2_C4tW3pd6qqaevYt62Vau7MiK5ryfzLGG4x6FaC86KEj1mJnJKX-hOWByCOU9RCm_Y2Cd9jMUmEx242OOk5Em2jix-b9sAW9FSNJnLgVFv08hJaW5DVPkG4I4hK7SzdbA3105ir2kHh9kdiJWc9KIOjDpqq_LZynqZwPuYw2gOiPDrgFWObShG8Mjyaw2CzE1IJA-PrWtFEwy6ESRZD6AwZYGzCL9Ro1p2pOfaQd0Hju78bxUFozvx9PJbCz07_QceFgylA.U9LCn4Y35IPbA5h8.sO9HP-tg30reLyc_Xed_uj-b3qOX-YHbsfMJDrCtGNKzwweurGA--FX7k3qTNG4VbBr7JINnZGVBzwYHNazzLsDzxRPElIPJquzdsht__NVlX94tYbol1zKyP0jxP6SlLKc3KqvfiQBts7VG4LST33T3FUIevBnc2gA2F0FpdG-iI0V1IrMy7lqxr3kNk8oe64UDTWka7BXbwvz8X-KOFC2oQvocOcLyiO4KlPMV4ZheORzW3kSLWM-Ko2mmRUKTGKIXczhrG6NCjoRLPhrbsva2t5bF30C786MR1eUcvaeSmVDpgh7QsUq2KwTiP9cuaPCpYC9Fa2JDgsGI_Qu6RszQFcQcC6OEWnWcdp8Mnoz0sCVUVZ9hsjR2rs5ukpAqrRsFt92y21l08ApBJTJW6-49EwHj46OGEkjOcImntZ4sLFMpC3_vZIAsFV5YhRDbiQVBcXjBo8vBYcUHXtKNQUZIqpxG3v_A6UHr9N-Mkp7CrH80NqW6e0GxbDQZLzpL2C6ciamc0_PruoDOz4YxpVYKPB0FF2wya9qdV627ppfJMZsKYIYci5psIDD65rR9Gj3QgLCLBh6byzWnIaToisnqqXGVsCWzUFt0S1btoIpMU5g3CgEh6jrkvxiRpkWBLQkbpK-Ie17NzgqpGDd2kr75lhpPDk6asps-VEzdS9N1XeDroq5lDhw-AT4kYswronpewskLHaFOUT25GkVQEYf4oh4o8ZorgTllTGCjs5dAx5hdKwC0TA-cJy6ygXuww76xWUv3YtfEBL3SEYuKCj0EXmf98Wog5PehgZGzqg8OFuqoCUxTPbO3Y7kxw8wKqmr3Kx2oPUfVHO7u5uoGOHbG_IYZG5KxtPk7gFslBaeKl6SK2FQD5XvfURvOKpB7HLq4vdbg7vPadrNYgygvQvc3zZGF_enQs15yBDqe8QmvZeTO-X4pys41tiirZLgddU6SmEtjV26vIQ1ycYpMVlXUAmOoxxHBbvzf_fHeSVOhmnOuXy4OvsMqcVWdbIPcnbTvIDqju7ieUimtZL0XW1Fs7yAqjHurxnR8s0TRecwUAxM4MhNBWmRpEhFFtvmxzSVp0FGejek6jz3mkl9uly6EdKwVLl6nZX4bICjKd4B3ZmCOCTdUEiPs_iczVPEoXg.04RIAR-tqZsHkss1ozTxfw",
        "TokenType": "Bearer",
        "AccessToken": "eyJraWQiOiJ3WDgxZ080QTdiNGUzTm1UTTVKNVk5eXpNb3lIYmw2NFo2ZFBlYU85UlkwPSIsImFsZyI6IlJTMjU2In0.eyJzdWIiOiJiMWJiYTI0Zi00OTEwLTQ2NjAtYjgwMC01OWY2Y2YwZGI2MDkiLCJ0b2tlbl91c2UiOiJhY2Nlc3MiLCJzY29wZSI6ImF3cy5jb2duaXRvLnNpZ25pbi51c2VyLmFkbWluIiwiaXNzIjoiaHR0cHM6XC9cL2NvZ25pdG8taWRwLnVzLWVhc3QtMS5hbWF6b25hd3MuY29tXC91cy1lYXN0LTFfUkZtMlVFa3BmIiwiZXhwIjoxNTAzODQ3Njc5LCJpYXQiOjE1MDM4NDQwNzksImp0aSI6ImU3ODA3NTIzLWRmYWEtNGNkOS1iOWU2LTgyMWJiNGRmYjA3MiIsImNsaWVudF9pZCI6IjdtanVqNGQ5bWpmOTZrYWR2YjJvazBmb282IiwidXNlcm5hbWUiOiJhbGV4MiJ9.Ml8KtJzkm2KAkMYnhZOQy5Od0wq6ZHhZh9-7K-Tnj9m3u-reLybmIDEcqxl1PMXZq-H6lFhzYdl_hnfmV1koZCJfwuMOg6Y-mDCW2_1NL9n6_mHsAQ3ruDJEbrzfJG-jYovgpCoFQ7TXi0a6x8Jy0c2LAHsGVx3p2-WJxGKbrJhjYiTpFbz0oouLn39Hc5EapXnusGvNIA9GLOKe7ATPrpTJ2MbJ0LxYwJKyJfl6U1uI9g0sD8JUysf05bG3NC_kIN57veqp3Sntn-oMO5Rp0hWBtj3fJWt17wKZPcqubVNxtbcBqBL6WLqFWOB4jvJr4eZpLcsk0ovvymU-mXR5aQ"
    },
    "ChallengeParameters": {}
}

aws cognito-idp admin-initiate-auth --user-pool-id us-east-1_RFm2UEkpf --client-id 7mjuj4d9mjf96kadvb2ok0foo6 --auth-flow ADMIN_NO_SRP_AUTH --auth-parameters 'USERNAME=alex,PASSWORD=Pa$$w0rd' --region us-east-1
{
    "AuthenticationResult": {
        "ExpiresIn": 3600,
        "IdToken": "eyJraWQiOiJESW9cL093QVF4NXYwTjJvTEM5azNzeXpUUllOa2RiMGZDbmJ3T1htZFNhbz0iLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJjYTQ2ZTQ5MS04OWY4LTQ0YmEtYTA0OS1kNzQ3YTk2MmQyYTQiLCJhdWQiOiI3bWp1ajRkOW1qZjk2a2FkdmIyb2swZm9vNiIsImVtYWlsX3ZlcmlmaWVkIjp0cnVlLCJ0b2tlbl91c2UiOiJpZCIsImF1dGhfdGltZSI6MTUwMzg0NDMwNiwiaXNzIjoiaHR0cHM6XC9cL2NvZ25pdG8taWRwLnVzLWVhc3QtMS5hbWF6b25hd3MuY29tXC91cy1lYXN0LTFfUkZtMlVFa3BmIiwiY29nbml0bzp1c2VybmFtZSI6ImFsZXgiLCJleHAiOjE1MDM4NDc5MDYsImlhdCI6MTUwMzg0NDMwNiwiZW1haWwiOiJhbGV4LmxpQHBydWRlbnRpYWwuY29tIn0.Qi2rFshzrtze4iNckMgiX24ldVyvx9paorxPQCvlLvCcMSoVsT_KWkEnlbIMGHysGuLkBzyR3YpRj2gM9bzbRty0oFGpU-dAvOgzhKSj2qtXqbgeAck6vFFQC-77QLsnKqVmrB-Czj1XeieNeA4y256MhudnLTtE0Y3AErK7nL79YTfWMdf2KA3hfiSob1dnaLQ5t9F9bPVlQBv9nt95oyk6b-3JBrVv7trrcSlMl6DLmS4Xl9SFZVBEGaxP-s8hC8-72NUZW9r-EFzcSeyG56clzy2_RTWxpBt7j7F_9ZOkTcZY8Ib2XoKX0aUtGv7K6cuL1eEuvgx5yUatD2_lPA",
        "RefreshToken": "eyJjdHkiOiJKV1QiLCJlbmMiOiJBMjU2R0NNIiwiYWxnIjoiUlNBLU9BRVAifQ.FB810MvPeRMgFqG3-YfkaoyKyNsBPXvc-KQ6-YPpXi8DRHYMUdBYe_laS7lUvpHBcm_nPvWl5P8aVTiazE2Ki-6kh5aQBCvNL_cZZA_DrUehoFaKT6fUogB_KxdQm2ccRGUg8htHEvF56kotExwcTJsXs3ahqq1si_t8JGsUPDKzKbvWcvBAO6dD3c7PNkzG-gU9KNFL7qWPQ-PdJQyiQWM5NIuUlh-DWzwmDQNuDcmX1hcIiJ1dcMr2CsFZXJB0-0aprXKrsB7yFQ28ybyEt6iSDr346_5weS8b_hp-pILqEBxIW956sDZO19RxxcIZOfa_3Jd4ufneyOyjpAfjZw.FjkhWTIwUU7l056V.GLQee9CJCZt1Yztze8uV9ljWBvSKyA4_aTa4cl_03hGZkcZtDLiUvGC3RWURUp71PIEZw-IDtanZyILWYwgkNcXLpO-9YOuIImF6w1Pw9ZjvmYcF6aC0P5RYWsnPog67lm2hYreX7VeeVYpTrjy_aesCOdoS1nL8pF7vFqYKUEapJdrc02UIVYEHoyf31mUfYZLAB8c6aHDq4kgizNA6RlYZQlWKswkhTND7jQOn6GkG3Gt6g7loq7xC3lWT2A4nzD7krIZOS4Pew1tdf0uM94CXIFzf6xyqNpKngxGCaz1BC2st5OBcdiFa7y8qXHZPq3RRWCAd0VcPTISwZY9cxZEWPf-6gtbYrIQYsbIFmnXDjxbVmPDvBY7_MIhk2s_fnDldU4McemWq9FqkCeLqy1hM0jJZuubGvqtKLqTLBu-BHhI-2AVMLCkdF54Hg7poNRmOLf1mgmH56j6SPI3Vf74DNcABqYQd1am1SlTrsTdBx8iY92MWbBu8b4zv2awpk4Iq8FRJBJFmA74VaEmv3iRXPGJHEgJk9Ih-QV702Qp_rBN9Bq9cyoPeED1Zccmj4iPzO9-rbKsihilYKqJzqUdChGpitRhALQq5Cmf-3GFlyM12C-Q5dMXrjo3lVYrMJD4f-DERsCDTO4jd_Lq67ltqXJKc7czvFe080yQFA7118hrXof6YBAJMT9RkWSBNh23a9KNGosTKTH_bungWz-DzWyxKZ3Ih45eJ1Q5Mdj35NnVNx0TUrHfEDzOJp-Nj_eK-xAyk-TSgd1-yl5jLr-vzw_SYNx1tWMjb6j-mzHnyTVABOZzXtpCDZfFeC3HCZzaCfzCiUbwPDxkp1O-HZPLdRPOCoKn9L1OsOhTSu0ooI9GxKeim2BgRz6fWfazKEyzgBo8uN-C9xyHpy_TwmBYkx_j8pjW0OLLjnTd94zRP91UyDRNhWnvnGdhsLFrvlKrQmiEp33EGKMpQuaHurajKf7c2SHTpB-j0wLdTX6i-oxKpGRS0OZFq-PV4S5BiQ4uU-UuNpMOreiDYthYDS-_RjDIIXMQ4tzrn2TUyZnUrFYDA7FtyyT3whD4IaCHexE0I2Ebw-pe75PNKmB-vAT5ittzKVTRWd7CP_5b-Qv-0zGpxIUi11VnzyXeRJ80.Deoxn8jJ5-QXa00M9G_XHA",
        "TokenType": "Bearer",
        "AccessToken": "eyJraWQiOiJ3WDgxZ080QTdiNGUzTm1UTTVKNVk5eXpNb3lIYmw2NFo2ZFBlYU85UlkwPSIsImFsZyI6IlJTMjU2In0.eyJzdWIiOiJjYTQ2ZTQ5MS04OWY4LTQ0YmEtYTA0OS1kNzQ3YTk2MmQyYTQiLCJ0b2tlbl91c2UiOiJhY2Nlc3MiLCJzY29wZSI6ImF3cy5jb2duaXRvLnNpZ25pbi51c2VyLmFkbWluIiwiaXNzIjoiaHR0cHM6XC9cL2NvZ25pdG8taWRwLnVzLWVhc3QtMS5hbWF6b25hd3MuY29tXC91cy1lYXN0LTFfUkZtMlVFa3BmIiwiZXhwIjoxNTAzODQ3OTA2LCJpYXQiOjE1MDM4NDQzMDYsImp0aSI6IjU2MGM5ZTlkLWFiMjctNGQ1OC1hMWVkLTFmY2M1MTVmMjNlYSIsImNsaWVudF9pZCI6IjdtanVqNGQ5bWpmOTZrYWR2YjJvazBmb282IiwidXNlcm5hbWUiOiJhbGV4In0.d9HG4GpxNMKKgnlI1lnq9g-F9zFZYyQkk9f_rUCwadE-HDyOpOjSGQwdRu9zR-zxCWj9xfffjdP0TYPO0wS9kL2BbRC4exw7iVZaZXaqIuAb061CtDFs3qv8CxCGi3MPluVijW3IwsbmYhEdSYLd9WuDHRLA21_tatyRiwctfjhjbiWvNve6kbTxODFS8Md4sk_jPYUpq4wS2k89148rSHG00Cx7P7ScLMI9D5hAFNr4dKpj0VsM5WhDWJ-8qYSgPMZc8rOC-TnZoJNKhJhm3DZHBars-1cKBRhPcyfUmN4h-I-TObPXdAwjFJ71sMgnycYfZYGSSxHn3wbdwAAPkQ"
    },
    "ChallengeParameters": {}
}

```

#### Lambda

 Create two lambda functions: EsLambda and AuthLambda
 
  * EsLambda: set environments ES_PROTOCOL, ES_HOST, ES_PORT to the Elasticsearch domain
  * AuthLambda: set environments APP_CLIENT_ID, USER_POOL_ID to the Cognito user pool
  * Build the es-lambda-example-1.0-SNAPSHOT.jar and upload to each lambda function
  * Set the handler as *example.handler.AuthHandler::handleRequest* and *example.handler.SearchHandler::handleRequest*
  * Each lambda has 1024M memory

#### IAM

 Create role for lambda to call Elasticsearch, and Cognito
 
  * Create a role with full AWSLambdaFullAccess, AWSLambdaExecute
  * Also attach policy of AmazonCognitoDeveloperAuthenticatedIdentities, and AmazonCognitoPowerUser

#### API Gateway
 Define endpoints
 
  * /search: call SearchLambda
  * /auth: call AuthLambda

## Test

* DataTest: loading data into Elasticsearch
* EndpointTest: test /auth and /search

## Performance

 Use JMeter for load testing, have done 20/sec user same input search (hot-start)
 
 * The average search is around 400ms (obviously, ES does cache result. The lambda normally takes 1 second)
 * The average auth took 1300ms (skewed by network. The lambda only takes 400ms)
 
 In case for a cold-start, each endpoints take about 3-4 seconds.

## Reference
* [AWS Elasticsearch service](https://aws.amazon.com/blogs/aws/new-amazon-elasticsearch-service/)
* [Java REST Client for elasticsearch](https://qbox.io/blog/rest-calls-new-java-elasticsearch-client-tutorial)
* [Elasticsearch Query](https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-multi-match-query.html)
* [Add User sign in and management to you apps with Cognito](https://www.youtube.com/watch?v=xLvZThZkWjI)
* [Iinvent 2016: Serverless auth and aothor: Identity Management](https://www.youtube.com/watch?v=n4hsWVXCuVI)
* [Serverless Reference Architecture: Web Application with Java](https://github.com/awslabs/lambda-refarch-webapp/tree/master/lambda-functions)
* [AWS Cognito idp client API - Java](http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/cognitoidp/AWSCognitoIdentityProviderClient.html)
