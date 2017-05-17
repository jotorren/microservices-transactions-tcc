package net.jotorren.microservices.rs;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

public class ExceptionRestHandler implements ExceptionMapper<Exception>{

	public class ErrorDetails {
		private int code;
		private String message;
		private String stack;
		
		public ErrorDetails(int code, String message, String stack){
			this.code = code;
			this.message = message;			
			this.stack = stack;
		}

		public int getCode() {
			return code;
		}

		public String getMessage() {
			return message;
		}
		
		public String getStack() {
			return stack;
		}
	}
	
	@Override
	public Response toResponse(Exception exception) {
		return toResponse(exception, Response.Status.INTERNAL_SERVER_ERROR);
	}
		
	public Response toResponse(Exception exception, Response.Status status) {
		return Response.status(status).entity(
				new ErrorDetails(status.getStatusCode(), exception.getMessage(), toString(exception))).build();
	}

	public String toString(Exception exception) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		exception.printStackTrace(pw);
		String stack = sw.toString();
		pw.close();
		
		return stack;
	}
}
