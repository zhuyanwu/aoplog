package com.zyw.aoplog.platform;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.*;

@Aspect
@Component
@Slf4j
public class OperLogAspect {

    @Autowired
    private ObjectMapper objectMapper;

    private ThreadLocal<Date> startTime = new ThreadLocal<>();


    @Pointcut("execution(* com.zyw.aoplog.platform.controller.*.*(..))")
    public void pointcut(){
    }

    @Before("pointcut()")
    public void doBefore(JoinPoint joinPoint){
        startTime.set(new Date());
    }

    @AfterReturning(pointcut = "pointcut()",returning = "rvt")
    public void doAfterReturning(JoinPoint joinPoint, Object rvt) throws Exception {
        HttpServletRequest request = getRequest();
        String uri = request.getRequestURI();
        Method method = getMethod(joinPoint);
        OperLog operLog = method.getAnnotation(OperLog.class);
        if(operLog != null){
            String operDesc = operLog.operDesc();
            String operModel = operLog.operModel();
            String operType = operLog.operType();
            log.info("operDesc : {}",operDesc);
            log.info("operModel : {}",operModel);
            log.info("operType : {}",operType);
        }
        // 获取请求的类名
        String className = joinPoint.getTarget().getClass().getName();
        // 获取请求的方法名
        String methodName = method.getName();
        // 请求的参数
        Map<String, String> reqMap = converMap(request.getParameterMap());
        String reqStr = objectMapper.writeValueAsString(reqMap);
        String responseResult = objectMapper.writeValueAsString(rvt);
        log.info("uri : {}",uri);
        log.info("className : {}",className);
        log.info("methodName : {}",methodName);
        log.info("reqStr : {}",reqStr);
        log.info("responseResult : {}",responseResult);
        log.info("startTime : {}",startTime.get());
        log.info("endTime : {}",new Date());
        Object[] args = joinPoint.getArgs();
        log.info("args : {}", args);
        startTime.remove();
        String ip = getClientIp(request);
        log.info("ip : {}",ip);


        LocalVariableTableParameterNameDiscoverer u = new LocalVariableTableParameterNameDiscoverer();
        String[] paramNames = u.getParameterNames(method);
        if (args != null && paramNames != null) {
            StringBuilder params = new StringBuilder();
            handleParams(params, args, Arrays.asList(paramNames));
            log.info("参数 : {}",params.toString());
        }

    }

    private Method getMethod(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return signature.getMethod();
    }

    private HttpServletRequest getRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes.getRequest();
    }

    public Map<String,String> converMap(Map<String, String[]> paramMap){
        Map<String,String> reMap = new HashMap<>();
        for (String key : paramMap.keySet()) {
            reMap.put(key,paramMap.get(key)[0]);
        }
        return reMap;
    }


    private StringBuilder handleParams(StringBuilder params, Object[] args, List paramNames) throws JsonProcessingException {
        for(int i = 0; i < args.length; ++i) {
            if (args[i] instanceof Map) {
                Set set = ((Map)args[i]).keySet();
                List list = new ArrayList();
                List paramList = new ArrayList();
                Iterator var8 = set.iterator();

                while(var8.hasNext()) {
                    Object key = var8.next();
                    list.add(((Map)args[i]).get(key));
                    paramList.add(key);
                }

                return this.handleParams(params, list.toArray(), paramList);
            }

            if (args[i] instanceof Serializable) {
                Class aClass = args[i].getClass();

                try {
                    aClass.getDeclaredMethod("toString", null);
                    params.append("  ").append(paramNames.get(i)).append(": ").append(this.objectMapper.writeValueAsString(args[i]));
                } catch (NoSuchMethodException var10) {
                    params.append("  ").append(paramNames.get(i)).append(": ").append(this.objectMapper.writeValueAsString(args[i].toString()));
                }
            } else if (args[i] instanceof MultipartFile) {
                MultipartFile file = (MultipartFile)args[i];
                params.append("  ").append(paramNames.get(i)).append(": ").append(file.getName());
            } else {
                params.append("  ").append(paramNames.get(i)).append(": ").append(args[i]);
            }
        }

        return params;
    }

    public String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Real-IP");
        if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Forwarded-For");
        }

        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }

        if (ip == null || ip.length() == 0 || "unknow".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }

        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    /**
     * 转换异常信息为字符串
     *
     * @param e
     */
    public String stackTraceToString(Exception e) {
        String exceptionName = e.getClass().getName();
        String exceptionMessage = e.getMessage();
        StackTraceElement[] elements = e.getStackTrace();
        StringBuffer strbuff = new StringBuffer();
        for (StackTraceElement stet : elements) {
            strbuff.append(stet + "\n");
        }
        String message = exceptionName + ":" + exceptionMessage + "\n\t" + strbuff.toString();
        return message;
    }

}
