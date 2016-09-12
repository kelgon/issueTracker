var loginedUser="";
var curPage=1;
var curChoice;
$("#loginnav").click(function () { $("#login").modal(); });
$("#tagnav").click(function() { $("#loading").modal(); getAllTags(); });
$("#newIssueBtn").click(function () {
	$("#createbtn").attr("class","btn btn-primary btn-block")
	$("#createbtn").html("提交");
	$("#newIssuePanel").modal();
});
$("#loginform").submit(login);
$("#chgpwdnav").click(function() {$("#chgpwd").modal()});
$("#logoutnav").click(logout);
$("#chgpwdbtn").click(chgpasswd);
$('#deadLineN').datetimepicker({format:"YYYY/MM/DD"});
$('#deadlineD').datetimepicker({format:"YYYY/MM/DD"});
$("#createbtn").click(createIssue);
$("#getCreatedBtn").click(function() {
	curPage = 1;
	curChoice = 'created';
	getIssues(curChoice, curPage); 
});
$("#getOwnedBtn").click(function() {
	curPage = 1;
	curChoice = 'owned';
	getIssues(curChoice, curPage); 
});
$("#getFollowedBtn").click(function() {
	curPage = 1;
	curChoice = 'followed';
	getIssues(curChoice, curPage); 
});
$("#getAllBtn").click(function() {
	curPage = 1;
	curChoice = 'all';
	getIssues(curChoice, curPage); 
});
$("#modifybtn").click(modifyIssue);
$("#closebtn").click(closeIssue);
$("#cancelbtn").click(cancelIssue);
$("#acknowledgebtn").click(ackIssue);
$("#updatebtn").click(updateIssue);
$("#completebtn").click(completeIssue);
$("#reopenbtn").click(reopenIssue);
$("#followbtn").click(followIssue);
$("#unfollowbtn").click(unfollowIssue);
$("#urgebtn").click(urgeIssue);
$("ul.pagination li:first a").click(function() {
	if($("ul.pagination li:first").hasClass("disabled"))
		return;
	getIssues(curChoice, --curPage);
});
$("ul.pagination li:last a").click(function() {
	if($("ul.pagination li:last").hasClass("disabled"))
		return;
	getIssues(curChoice, ++curPage);
});
$("#newtagbtn").click(createTag);


$("#loading").modal();

$.ajax({
	url: "s", 
	method: "get",
	data: "bn=us&mn=passive",
	cache: false, 
	dataType: "json",
	contentType: "application/x-www-form-urlencoded; charset=utf-8",
	success: function(data) {
		if(data.retCode=="0") {
			$("#usernav > a").prepend(data.ext);
			$("#usernav").show();
			$("#loginnav").hide();
			loginedUser=data.ext;
			initTypeahead();
		} else {
			$("#loading").modal("hide");
			$("#login").modal();
		}
	}
});

function initTypeahead() {
	$.ajax({
		url: "s", 
		method: "get",
		data: "bn=cs&mn=initTypeahead",
		cache: false, 
		dataType: "json",
		contentType: "application/x-www-form-urlencoded; charset=utf-8",
		success: function(data) {
			$("#loading").modal("hide");
			$("input[name='ownerN']").typeahead({source:data.users});
			$("input[name='ownerS']").typeahead({source:data.users});
			$("input[name='creatorS']").typeahead({source:data.users});
			$("input[name='ownerD']").typeahead({source:data.users});
			$("input[name='creatorD']").typeahead({source:data.users});
			tagsTypeahead("N", data.tags);
			tagsTypeahead("D", data.tags);
			tagsTypeahead("S", data.tags);
		}
	});
}

function tagsTypeahead(t, source) {
	var tagDiv = "tagDiv"+t;
	var tagInput = "addTags"+t;
	$("#"+tagInput).typeahead({
		source: source,
		showHintOnFocus: true,
		updater: function (item) {
			var tag = $("<span class='label label-primary tag' title='点击移除'>"+item+"</span>");
			tag.click(function() {
				$(this).remove();
			})
			var tags = $("#"+tagDiv+" span");
			if(tags.length >= 5) {
				alert("最多可以设置5个标签");
				return "";
			}
			var has = false;
			for(var i=0; i<tags.length; i++) {
				if(tag.html() == $(tags.get(i)).html()) {
					has = true;
					break;
				}
			}
			if(!has) {
				$("#"+tagDiv).append(tag);
			}
			$("#"+tagInput).blur();
			return "";
		}
	});
	$("#"+tagInput).blur(function (){
		$(this).typeahead('hide');
	});
}

function login() {
	$("#loginbtn").html("登录中...");
	var userName = $("input[name='userName']").val();
	$.ajax({
		url: "s", 
		method: "post",
		data: "bn=us&mn=login&userName="+userName+"&pwd="+$("input[name='pwd']").val(),
		cache: false, 
		dataType: "json",
		contentType: "application/x-www-form-urlencoded; charset=utf-8",
		success: function(data) {
			if(data.retCode == "0") {
				$("#loginbtn").html("登录成功");
				$("#login").modal('hide');
				$("#usernav > a").prepend(data.retMsg);
				$("#usernav").show();
				$("#loginnav").hide();
				$.cookie('authToken', userName+","+data.ext, {expires: 30});
				loginedUser=data.retMsg;
				initTypeahead();
			} else if(data.retCode == "3") {
				$("#loginMsg").html("登录尝试过于频繁，请稍后再试");
				$("#loginMsg").show();
				$("#loginbtn").html("登录");
			} else {
				$("#loginMsg").html("用户名或密码错误");
				$("#loginMsg").show();
				$("#loginbtn").html("登录");
			}
		}
	});
	return false;
}

function logout() {
	$("#loading").modal();
	$.removeCookie('authToken');
	$.ajax({
		url: "s", 
		method: "post",
		data: "bn=us&mn=logout",
		cache: false, 
		dataType: "json",
		contentType: "application/x-www-form-urlencoded; charset=utf-8",
		success: function(data) {
			window.location.reload();
		}
	});
}

function chgpasswd() {
	var oldpwd = $("input[name='oldpwd'");
	var newpwd = $("input[name='newpwd'");
	var newpwd2 = $("input[name='newpwd2'");
	if($.trim(oldpwd.val()) == "") {
		$("#chgpwdMsg").html("请填写旧密码");
		$("#chgpwdMsg").show();
		return;
	}
	if(newpwd.val().length < 6 || newpwd.val().length>20) {
		$("#chgpwdMsg").html("密码长度6-20位");
		$("#chgpwdMsg").show();
		return;
	}
	if(newpwd.val() != newpwd2.val()) {
		$("#chgpwdMsg").html("两次密码输入不一致");;
		$("#chgpwdMsg").show();
		return;
	}
	$("#chgpwdbtn").prop("disabled",true);
	$.ajax({
		url: "s", 
		method: "post",
		data: "bn=us&mn=chgpasswd&oldpwd="+oldpwd.val()+"&newpwd="+newpwd.val(),
		cache: false, 
		dataType: "json",
		contentType: "application/x-www-form-urlencoded; charset=utf-8",
		success: function(data) {
			$("#chgpwdbtn").prop("disabled",false);
			if(data.retCode=="0") {
				alert("密码修改成功");
				$("#chgpwd").modal("hide");
			} else {
				$("#chgpwdMsg").html(data.retMsg);
				$("#chgpwdMsg").show();
			}
		}
	});
}

function createIssue() {
	var issueTitle = $("input[name='issueTitleN']");
	var owner = $("input[name='ownerN']");
	var remindFreq = $("input[name='remindFreqN']");
	var deadline = $("#deadLineN");
	var issueDetail = $("textarea[name='issueDetailN']");
	var reg = new RegExp("^[\u4e00-\u9fa5]{2,4}[\(][a-zA-Z0-9]+[\)]$", "g");
	var valid = true;
	if(owner.val().match(reg) == null) {
		owner.parent().addClass("has-error");
		$("#createmsg").html("请使用自动完成功能填写负责人，不要手工填写");
		$("#createmsg").show();
		valid = false;
	}
	if(issueTitle.val() == "") {
		issueTitle.parent().addClass("has-error");
		issueTitle.attr("placeholder","请填写");
		valid = false;
	}
	if(deadline.val() == "") {
		deadline.parent().addClass("has-error");
		deadline.attr("placeholder","请填写");
		valid = false;
	}
	var tags = $("#tagDivN span");
	var tagsP = "";
	for(var i=0; i<tags.length; i++) {
		tagsP += $(tags.get(i)).html();
		if(i < tags.length-1)
			tagsP += ",";
	}
	if(valid) {
		$("#createbtn").html("提交中...");
		$.ajax({
			url: "s", 
			method: "post",
			data: "bn=is&mn=newIssue&issueTitle="+issueTitle.val()+"&owner="+owner.val()+"&remindFreq="
				+remindFreq.val()+"&deadline="+deadline.val()+"&issueDetail="+issueDetail.val()+"&tags="+tagsP,
			cache: false, 
			dataType: "json",
			contentType: "application/x-www-form-urlencoded; charset=utf-8",
			success: function(data) {
				if(data.retCode=="0") {
					$("#createbtn").html("提交成功");
					$("#createbtn").toggleClass("btn-success");
					$("#newIssuePanel").modal('hide');
					getIssues("created");
				} else {
					$("#createmsg").html("提交失败，请稍后重试");
					$("#createmsg").show();
					$("#createbtn").html("提交");
				}
			}
		});
	}
}

function getIssues(choice, page) {
	$("#loading").modal();
	var tags = $("#tagDivS span");
	var tagsP = "";
	for(var i=0; i<tags.length; i++) {
		tagsP += $(tags.get(i)).html();
		if(i < tags.length-1)
			tagsP += ",";
	}
	var data = "bn=is&mn=getIssues&choice="+choice+"&tags="+tagsP+"&page="+page;
	$.ajax({
		url: "s", 
		method: "post",
		data: data,
		cache: false, 
		dataType: "json",
		contentType: "application/x-www-form-urlencoded; charset=utf-8",
		success: function(data) {
			if(data.retCode == "0") {
				var table = $("#issueTable").children("tbody");
				table.empty();
				for(var i=0; i<data.ext.length; i++) {
					var issue = data.ext[i];
					var tags = "";
					if(issue.tags != undefined && issue.tags != "") {
						for(var j=0; j<issue.tags.length; j++)
							tags+= "<span class='label label-primary'>"+issue.tags[j]+"</span> ";
					}
					table.append("<tr class='istable'><td><input type='hidden' value='"+issue.oid+"'/>"+issue.title+" "+tags+
							"</td><td>"+issue.creator+"</td><td>"+issue.owner+"</td><td>"+issue.state+"</td><td>"+
							issue.createDate+"</td><td>"+issue.deadline+"</td></tr>");
				}
				$(".istable").click(showIssue);
				$("ul.pagination li.active a").html(page);
				if(page == 1) 
					$("ul.pagination li:first").addClass("disabled");
				else
					$("ul.pagination li:first").removeClass("disabled");
				if(data.ext.length < 10)
					$("ul.pagination li:last").addClass("disabled");
				else
					$("ul.pagination li:last").removeClass("disabled");
				$("ul.pagination").show();
				$("#loading").modal("hide");
			}
		}
	});
}

function showIssue() {
	$("#loading").modal();
	$("#issueDetailPanel input[type='text']").prop("readonly",true);
	$("#issueDetailPanel textarea").prop("readonly",true);
	$("#addTagsD").prop("disabled",true);
	$("#modifybtn").html("修改");
	$("#updatemsg").hide();
	$("#creatorOps").hide();
	$("#ownerOps").hide();
	$("#otherOps").hide();
	modifyOpen=false;
	updateOpen=false;
	var oid = $(this).children(":first").children("input").val();
	$.ajax({
		url: "s", 
		method: "get",
		data: "bn=is&mn=getIssueById&oid="+oid,
		cache: false, 
		dataType: "json",
		contentType: "application/x-www-form-urlencoded; charset=utf-8",
		success: function(data) {
			$("#loading").modal("hide");
			if(data.retCode == "0") {
				$("input[name='issueIdD']").val(data.ext.oid);
				$("#creatorD").html("由 "+data.ext.creator+" 于 "+data.ext.createDate+" 创建");
				$("input[name='issueTitleD']").val(data.ext.title);
				$("input[name='ownerD']").val(data.ext.owner);
				$("input[name='remindFreqD']").val(data.ext.remindFreq);
				$("#deadlineD").val(data.ext.deadline);
				$("textarea[name='issueDetailD']").val(data.ext.detail);
				$("input[name='stateD']").val(data.ext.state);
				$("textarea[name='progressD']").val(data.ext.progress);
				
				$("input[name='followerD']").val("");
				if(data.ext.follower != undefined) {
					var followers = "";
					var followed = false;
					for(var i=0; i<data.ext.follower.length; i++) {
						followers += data.ext.follower[i] + " ";
						if(loginedUser == data.ext.follower[i])
							followed = true;
					}
					$("input[name='followerD']").val(followers);
				}
				
				$("#tagDivD").html("");
				if(data.ext.tags != undefined && data.ext.tags != "") {
					for(var i=0; i<data.ext.tags.length; i++) {
						var tag = $("<span class='label label-primary tag'>"+data.ext.tags[i]+"</span>");
						$("#tagDivD").append(tag);
					}
				}
				
				checkBtnEnable(data.ext.state);
				
				if(loginedUser == data.ext.creator)
					$("#creatorOps").show();
				if(loginedUser == data.ext.owner)
					$("#ownerOps").show();
				if(loginedUser != data.ext.owner && loginedUser != data.ext.creator)
					$("#otherOps").show();
				if(followed) {
					$("#followbtn").prop("disabled",true);
				} else {
					$("#unfollowbtn").prop("disabled",true);
				}
				$("#issueDetailPanel").modal();
			} else {
				alert(data.retMsg);
			}
		}
	})
}

function checkBtnEnable(state) {
	$("#issueDetailPanel button.btn").prop("disabled",false);
	if(state == "已关闭") {
		$("#issueDetailPanel button.btn").prop("disabled",true);
		$("#reopenbtn").prop("disabled",false);
	}
	if(state == "已取消") {
		$("#issueDetailPanel button.btn").prop("disabled",true);
	}
	if(state == "已接收") {
		$("#acknowledgebtn").prop("disabled",true);
		$("#reopenbtn").prop("disabled",true);
	}
	if(state == "进行中") {
		$("#acknowledgebtn").prop("disabled",true);
		$("#reopenbtn").prop("disabled",true);
	}
	if(state == "已完成") {
		$("#acknowledgebtn").prop("disabled",true);
		$("#updatebtn").prop("disabled",true);
		$("#completebtn").prop("disabled",true);
		$("#urgebtn").prop("disabled",true);
	}
	if(state == "新建" || state == "重开") {
		$("#updatebtn").prop("disabled",true);
		$("#completebtn").prop("disabled",true);
		$("#reopenbtn").prop("disabled",true);
	}
}

var modifyOpen=false;
function modifyIssue() {
	if(!modifyOpen) {
		$("input[name='remindFreqD'").prop("readonly",false);
		$("#deadlineD").prop("readonly",false);
		$("textarea[name='issueDetailD'").prop("readonly",false);
		$("#addTagsD").prop("disabled",false);
		$("#addTagsD").prop("readonly",false);
		$("#issueDetailPanel .tag").click(function() {
			$(this).remove();
		})
		modifyOpen = true;
		$("#issueDetailPanel button.btn").prop("disabled",true);
		$("#modifybtn").html("提交");
		$("#modifybtn").prop("disabled",false);
	} else {
		$("#modifybtn").html("提交中...");
		$("#modifybtn").prop("disabled", true);
		var tags = $("#tagDivD span");
		var tagsP = "";
		for(var i=0; i<tags.length; i++) {
			tagsP += $(tags.get(i)).html();
			if(i < tags.length-1)
				tagsP += ",";
		}
		$.ajax({
			url: "s", 
			method: "post",
			data: "bn=is&mn=modifyIssue&oid="+$("input[name='issueIdD']").val()+"&remindFreq="
				+$("input[name='remindFreqD']").val()+"&deadline="+$("#deadlineD").val()+
				"&issueDetail="+$("textarea[name='issueDetailD'").val()+"&tags="+tagsP,
			cache: false, 
			dataType: "json",
			contentType: "application/x-www-form-urlencoded; charset=utf-8",
			success: function(data) {
				$("#modifybtn").prop("disabled", false);
				if(data.retCode == "0") {
					$("#issueDetailPanel input[type='text']").prop("readonly",true);
					$("#issueDetailPanel textarea").prop("readonly",true);
					$("#modifybtn").html("修改");
					checkBtnEnable($("input[name='stateD']").val());
					modifyOpen = false;
				} else {
					$("#updatemsg").html(data.retMsg);
					$("#updatemsg").show();
				}
			}
		});
	}
}



function closeIssue() {
	$("#closebtn").html("提交中...");
	$("#closebtn").prop("disabled",true);
	$.ajax({
		url: "s", 
		method: "post",
		data: "bn=is&mn=closeIssue&oid="+$("input[name='issueIdD']").val(),
		cache: false, 
		dataType: "json",
		contentType: "application/x-www-form-urlencoded; charset=utf-8",
		success: function(data) {
			$("#closebtn").prop("disabled",false);
			if(data.retCode == "0") {
				$("#closebtn").html("关闭");
				$("input[name='stateD'").val("已关闭");
				checkBtnEnable($("input[name='stateD']").val());
			} else {
				$("#updatemsg").html(data.retMsg);
				$("#updatemsg").show();
			}
		}
	});
}

function cancelIssue() {
	$("#cancelbtn").html("提交中...");
	$.ajax({
		url: "s", 
		method: "post",
		data: "bn=is&mn=cancelIssue&oid="+$("input[name='issueIdD']").val(),
		cache: false, 
		dataType: "json",
		contentType: "application/x-www-form-urlencoded; charset=utf-8",
		success: function(data) {
			$("#issueDetailPanel").modal("hide");
			if(data.retCode == "0") {
				$("#loading").modal("hide");
			} else {
				$("#updatemsg").html(data.retMsg);
				$("#updatemsg").show();
			}
		}
	});
}

function ackIssue() {
	$("#acknowledgebtn").html("提交中...");
	$("#acknowledgebtn").prop("disabled",true);
	$.ajax({
		url: "s", 
		method: "post",
		data: "bn=is&mn=ackIssue&oid="+$("input[name='issueIdD']").val(),
		cache: false, 
		dataType: "json",
		contentType: "application/x-www-form-urlencoded; charset=utf-8",
		success: function(data) {
			$("#acknowledgebtn").html("确认接收");
			$("#acknowledgebtn").prop("disabled",false);
			if(data.retCode == "0") {
				$("input[name='stateD'").val("已接收");
				checkBtnEnable($("input[name='stateD']").val());
			} else {
				$("#updatemsg").html(data.retMsg);
				$("#updatemsg").show();
			}
		}
	});
}

var updateOpen=false;
function updateIssue() {
	if(!updateOpen) {
		$("textarea[name='progressD']").prop("readonly",false);
		if($("textarea[name='progressD']").val()=="")
			$("textarea[name='progressD']").val(getDate()+" 进展：");
		else
			$("textarea[name='progressD']").val($("textarea[name='progressD']").val()+"\r\n"+getDate()+" 进展：");
		updateOpen = true;
		$("#issueDetailPanel button.btn").prop("disabled",true);
		$("#updatebtn").html("提交");
		$("#updatebtn").prop("disabled",false);
	} else {
		$("#updatebtn").html("提交中...");
		$("#updatebtn").prop("disabled",true);
		$.ajax({
			url: "s", 
			method: "post",
			data: "bn=is&mn=updateIssue&oid="+$("input[name='issueIdD']").val()+"&progress="+
				$("textarea[name='progressD']").val(),
			cache: false, 
			dataType: "json",
			contentType: "application/x-www-form-urlencoded; charset=utf-8",
			success: function(data) {
				$("#updatebtn").prop("disabled",false);
				if(data.retCode == "0") {
					$("textarea[name='progressD']").prop("readonly",true);
					$("#updatebtn").html("更新进展");
					$("input[name='stateD']").val("进行中");
					updateOpen = false;
					checkBtnEnable($("input[name='stateD']").val());
				} else {
					$("#updatemsg").html(data.retMsg);
					$("#updatemsg").show();
				}
			}
		});
	}
}

function completeIssue() {
	if(!updateOpen) {
		$("textarea[name='progressD']").prop("readonly",false);
		if($("textarea[name='progressD']").val()=="")
			$("textarea[name='progressD']").val(getDate()+" 反馈完成：");
		else
			$("textarea[name='progressD']").val($("textarea[name='progressD']").val()+"\r\n"+getDate()+" 反馈完成：");
		updateOpen = true;
		$("#issueDetailPanel button.btn").prop("disabled",true);
		$("#completebtn").html("提交");
		$("#completebtn").prop("disabled",false);
	} else {
		$("#completebtn").html("提交中...");
		$("#completebtn").prop("disabled",true);
		$.ajax({
			url: "s", 
			method: "post",
			data: "bn=is&mn=completeIssue&oid="+$("input[name='issueIdD']").val()+"&progress="+
				$("textarea[name='progressD']").val(),
			cache: false, 
			dataType: "json",
			contentType: "application/x-www-form-urlencoded; charset=utf-8",
			success: function(data) {
				$("#completebtn").prop("disabled",false);
				if(data.retCode == "0") {
					$("textarea[name='progressD']").prop("readonly",true);
					$("#completebtn").html("反馈完成");
					$("input[name='stateD']").val("已完成");
					updateOpen = false;
					checkBtnEnable($("input[name='stateD']").val());
				} else {
					$("#updatemsg").html(data.retMsg);
					$("#updatemsg").show();
				}
			}
		});
	}
}

function reopenIssue() {
	if(!modifyOpen) {
		$("textarea[name='issueDetailD']").prop("readonly",false);
		if($("textarea[name='issueDetailD']").val()=="")
			$("textarea[name='issueDetailD']").val(getDate()+" 重开：");
		else
			$("textarea[name='issueDetailD']").val($("textarea[name='issueDetailD']").val()+"\r\n"+getDate()+" 重开：");
		modifyOpen = true;
		$("#reopenbtn").html("提交");
		$("#issueDetailPanel button.btn").prop("disabled",true);
		$("#reopenbtn").prop("disabled",false);
	} else {
		$("#reopenbtn").html("提交中...");
		$("#reopenbtn").prop("disabled",true);
		$.ajax({
			url: "s", 
			method: "post",
			data: "bn=is&mn=reopenIssue&oid="+$("input[name='issueIdD']").val()+"&issueDetail="+
				$("textarea[name='issueDetailD']").val(),
			cache: false, 
			dataType: "json",
			contentType: "application/x-www-form-urlencoded; charset=utf-8",
			success: function(data) {
				$("#reopenbtn").prop("disabled",false);
				if(data.retCode == "0") {
					$("textarea[name='issueDetailD']").prop("readonly",true);
					$("#reopenbtn").html("重开");
					$("input[name='stateD']").val("重开");
					checkBtnEnable($("input[name='stateD']").val());
					modifyOpen = false;
				} else {
					$("#updatemsg").html(data.retMsg);
					$("#updatemsg").show();
				}
			}
		});
	}
}

function followIssue() {
	$("#followbtn").html("提交中...");
	$.ajax({
		url: "s", 
		method: "post",
		data: "bn=is&mn=followIssue&oid="+$("input[name='issueIdD']").val(),
		cache: false, 
		dataType: "json",
		contentType: "application/x-www-form-urlencoded; charset=utf-8",
		success: function(data) {
			$("#followbtn").html("关注");
			if(data.retCode == "0") {
				if(data.ext != undefined) {
					var followers = "";
					var followed = false;
					for(var i=0; i<data.ext.length; i++) {
						var f = data.ext[i].name+"("+data.ext[i].userName+")";
						followers += f + " ";
						if(loginedUser == f)
							followed = true;
					}
					if(followed) {
						$("#followbtn").prop("disabled",true);
						$("#unfollowbtn").prop("disabled",false);
					} else {
						$("#followbtn").prop("disabled",false);
						$("#unfollowbtn").prop("disabled",true);
					}
					$("input[name='followerD']").val(followers);
				} else {
					$("input[name='followerD']").val("");
				}
			} else {
				$("#updatemsg").html(data.retMsg);
				$("#updatemsg").show();
			}
		}
	});
}

function unfollowIssue() {
	$("#unfollowbtn").html("提交中...");
	$.ajax({
		url: "s", 
		method: "post",
		data: "bn=is&mn=unfollowIssue&oid="+$("input[name='issueIdD']").val(),
		cache: false, 
		dataType: "json",
		contentType: "application/x-www-form-urlencoded; charset=utf-8",
		success: function(data) {
			$("#unfollowbtn").html("取消关注");
			if(data.retCode == "0") {
				if(data.ext != undefined) {
					var followers = "";
					var followed = false;
					for(var i=0; i<data.ext.length; i++) {
						var f = data.ext[i].name+"("+data.ext[i].userName+")";
						followers += f + " ";
						if(loginedUser == f)
							followed = true;
					}
					if(followed) {
						$("#followbtn").prop("disabled",true);
						$("#unfollowbtn").prop("disabled",false);
					} else {
						$("#followbtn").prop("disabled",false);
						$("#unfollowbtn").prop("disabled",true);
					}
					$("input[name='followerD']").val(followers);
				} else {
					$("input[name='followerD']").val("");
				}
			} else {
				$("#updatemsg").html(data.retMsg);
				$("#updatemsg").show();
			}
		}
	});
}

function urgeIssue() {
	$("#urgebtn").html("提交中...");
	$.ajax({
		url: "s", 
		method: "post",
		data: "bn=is&mn=urgeIssue&oid="+$("input[name='issueIdD']").val(),
		cache: false, 
		dataType: "json",
		contentType: "application/x-www-form-urlencoded; charset=utf-8",
		success: function(data) {
			$("#urgebtn").html("催促");
			if(data.retCode == "0") {
				alert("催促信息已发送");
			} else {
				$("#updatemsg").html(data.retMsg);
				$("#updatemsg").show();
			}
		}
	});
}

function getAllTags() {
	$.ajax({
		url: "s", 
		method: "get",
		data: "bn=ts&mn=getAllTags",
		cache: false, 
		dataType: "json",
		contentType: "application/x-www-form-urlencoded; charset=utf-8",
		success: function(data) {
			if(data.retCode=="0") {
				var table = $("#tagTable").children("tbody");
				table.empty();
				for(var i=0; i<data.ext.length; i++) {
					var tag = data.ext[i];
					var delbtn = $("<button type='button' class='btn btn-danger btn-block'>删除</button>");
					delbtn.click(function() {
						var oid = $(this).prev().val();
						$(this).prop("disabled",true);
						$.ajax({
							url: "s", 
							method: "post",
							data: "bn=ts&mn=deleteTag&oid="+oid,
							cache: false, 
							dataType: "json",
							contentType: "application/x-www-form-urlencoded; charset=utf-8",
							success: function(data) {
								if(data.retCode == "0") {
									getAllTags();
								}
							}
						});
					});
					var tr = $("<tr></tr>");
					tr.append("<td>"+tag.name+"</td>");
					var td = $("<td><input type='hidden' value='"+tag.oid+"'/></td>");
					td.append(delbtn);
					tr.append(td);
					table.append(tr);
				}
				$("#loading").modal("hide");
				$("#tagManagePanel").modal();
			}
		}
	});
}

function createTag() {
	var tag = $("input[name='tagN'").val();
	if(tag == "")
		return;
	else {
		$("#newtagbtn").prop("disabled",true);
		$.ajax({
			url: "s", 
			method: "post",
			data: "bn=ts&mn=createTag&tag="+tag,
			cache: false, 
			dataType: "json",
			contentType: "application/x-www-form-urlencoded; charset=utf-8",
			success: function(data) {
				$("#newtagbtn").prop("disabled", false);
				if(data.retCode == "0")
					getAllTags();
				else {
					$("#tagMsg").html(data.retMsg);
					$("#tagMsg").show();
				}
			}
		});
	}
}

Date.prototype.Format = function(fmt)   
{ //author: meizz   
  var o = {   
    "M+" : this.getMonth()+1,                 //月份   
    "d+" : this.getDate(),                    //日   
    "h+" : this.getHours(),                   //小时   
    "m+" : this.getMinutes(),                 //分   
    "s+" : this.getSeconds(),                 //秒   
    "q+" : Math.floor((this.getMonth()+3)/3), //季度   
    "S"  : this.getMilliseconds()             //毫秒   
  };   
  if(/(y+)/.test(fmt))   
    fmt=fmt.replace(RegExp.$1, (this.getFullYear()+"").substr(4 - RegExp.$1.length));   
  for(var k in o)   
    if(new RegExp("("+ k +")").test(fmt))   
  fmt = fmt.replace(RegExp.$1, (RegExp.$1.length==1) ? (o[k]) : (("00"+ o[k]).substr((""+ o[k]).length)));   
  return fmt;   
}  

function getDate() {
	var date = new Date();
	return date.Format("yyyy/MM/dd hh:mm");
}