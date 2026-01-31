const toggleSidebar = () => {
	if($(".sidebar").is(":visible")) {
		$(".sidebar").css("display", "none");
		$(".main-content").css("margin-left", "0");}
		else{
			$(".sidebar").css("display", "block");
			$(".main-content").css("margin-left", "20%");
		}
		}
		

const currentPage = document.getElementById("currentPage").value;
		
function search  ()  {
	
	let  query = $("#searchInput").val();
	
	if(query == "") {
		$(".search-results").hide();
	}else {
		console.log(query);
		
		let url = `http://localhost:8080/search/${query}`;
		
	fetch(url).then((response) => {
		return response.json();
		}
			).then((data) => {
				
		
		let text = "<div class='list-group'>";
		
		data.forEach((contact) => {
			text += `<a href='/user/${contact.cid}/contact-show' class='list-group-item list-group-item-action'>${contact.name}</a>`;
		});
		text += '</div>';
		
		$(".search-results").html(text);
		$(".search-results").show();
	
		});
		
		
		}
		};